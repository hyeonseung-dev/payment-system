package com.example.paymentsystem.domain.refund.service;

import com.example.paymentsystem.domain.order.entity.OrderItem;
import com.example.paymentsystem.domain.order.repository.OrderItemRepository;
import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.domain.payment.service.PaymentService;
import com.example.paymentsystem.domain.product.entity.Product;
import com.example.paymentsystem.domain.product.repository.ProductRepository;
import com.example.paymentsystem.domain.refund.dto.RefundItemRequest;
import com.example.paymentsystem.domain.refund.dto.RefundResponse;
import com.example.paymentsystem.domain.refund.entity.Refund;
import com.example.paymentsystem.domain.refund.entity.RefundItem;
import com.example.paymentsystem.domain.refund.entity.RefundStatus;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import com.example.paymentsystem.infra.portone.client.PortOneIdempotencyKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 환불 요청 선점과 환불 완료 후 내부 DB 상태 변경을 처리하는 서비스이다.
 *
 * <p>PortOne 환불 요청 전에 REQUESTED 환불과 환불 상품을 먼저 저장해 같은 결제의 중복 환불 요청이
 * PortOne까지 나가지 못하도록 막는다. PortOne 같은 외부 API 호출은 Facade에서 수행한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundCommandService {

    private final PaymentService paymentService;
    private final RefundService refundService;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    /**
     * 환불 요청을 REQUESTED 상태로 저장하고 환불 수량을 선점한다.
     *
     * <p>같은 결제에 대한 동시 환불 요청을 막기 위해 Payment에 비관적 락을 걸고,
     * PortOne 환불 API 호출 전에 RefundItem을 저장해 환불 수량을 먼저 선점한다.</p>
     *
     * @param memberId 인증 회원 ID
     * @param paymentId 결제 ID
     * @param reason 환불 사유
     * @param items 환불 요청 상품 목록
     * @return PortOne 환불 요청에 필요한 선점 환불 정보
     */
    @Transactional
    public RequestedRefundResult requestRefund(
            Long memberId,
            Long paymentId,
            String reason,
            List<RefundItemRequest> items
    ) {
        // 같은 결제에 대한 환불 요청이 동시에 처리되지 않도록 Payment row에 비관적 락을 건다.
        Payment payment = paymentService.findByIdWithOrderAndMemberForUpdate(paymentId);
        log.info("환불 요청 선점 시작: memberId={}, orderId={}, paymentId={}, paymentStatus={}",
                memberId, payment.getOrder().getId(), paymentId, payment.getStatus());

        // 요청 회원이 결제 주문의 소유자인지 검증해 타인 결제 환불을 차단한다.
        validateOwnership(payment, memberId);

        // 결제가 PAID 또는 PARTIAL_REFUNDED 상태인지 검증한다.
        refundService.validateRefundablePayment(payment);

        // 같은 요청 안에 동일 orderItemId가 중복으로 들어와 수량 검증을 우회하지 못하게 막는다.
        validateDuplicateOrderItems(items);

        // 클라이언트가 보낸 금액은 신뢰하지 않고 주문 상품 스냅샷과 결제 비율로 서버에서 환불 금액을 계산한다.
        RefundAmount refundAmount = calculateRefundAmount(payment, items);
        String idempotencyKey = PortOneIdempotencyKeyGenerator.generateIdempotencyKey();

        // PortOne 호출 전에 REQUESTED 환불을 먼저 저장해 환불 요청 1건과 멱등성 키를 기록한다.
        Refund refund = refundService.createRequestedRefund(
                payment,
                reason,
                refundAmount.totalRefundAmount(),
                refundAmount.pointRefundAmount(),
                refundAmount.pgRefundAmount(),
                refundAmount.earnedPointCancelAmount(),
                refundAmount.earnedPointDeductionAmount(),
                idempotencyKey
        );

        // 환불 요청 상품별 포인트/PG 환불 금액을 배분한다. 마지막 상품에는 나눗셈 잔액을 몰아 합계를 맞춘다.
        Long remainingPointRefundAmount = refundAmount.pointRefundAmount();
        Long remainingPgRefundAmount = refundAmount.pgRefundAmount();

        for (int index = 0; index < items.size(); index++) {
            RefundItemRequest itemRequest = items.get(index);
            OrderItem orderItem = findOrderItemForRefund(payment, itemRequest.orderItemId());
            boolean isLastItem = index == items.size() - 1;

            Long itemTotalAmount = calculateItemRefundAmount(orderItem, itemRequest.quantity());
            Long itemPointRefundAmount = isLastItem
                    ? remainingPointRefundAmount
                    : calculateProportionalAmount(
                            itemTotalAmount,
                            refundAmount.pointRefundAmount(),
                            refundAmount.totalRefundAmount()
                    );
            Long itemPgRefundAmount = isLastItem
                    ? remainingPgRefundAmount
                    : itemTotalAmount - itemPointRefundAmount;

            refundService.createRefundItem(
                    refund,
                    orderItem,
                    itemRequest.quantity(),
                    itemPointRefundAmount,
                    itemPgRefundAmount
            );

            // 다음 상품 배분 시 남은 금액을 기준으로 마지막 상품의 잔액을 보정한다.
            remainingPointRefundAmount -= itemPointRefundAmount;
            remainingPgRefundAmount -= itemPgRefundAmount;
        }

        log.info("환불 요청 선점 완료: orderId={}, paymentId={}, refundId={}, pgRefundAmount={}",
                payment.getOrder().getId(), paymentId, refund.getId(), refundAmount.pgRefundAmount());

        return new RequestedRefundResult(
                refund.getId(),
                payment.getPortonePaymentId(),
                refundAmount.pgRefundAmount(),
                idempotencyKey
        );
    }

    /**
     * REQUESTED 환불을 완료 처리하고 재고, 포인트, 결제 상태를 반영한다.
     *
     * <p>PortOne 환불 성공 이후에만 호출되어야 하며, 이 메서드 안에서 내부 DB 상태를
     * 하나의 트랜잭션으로 변경한다.</p>
     *
     * @param refundId 환불 ID
     * @return 환불 응답
     */
    @Transactional
    public RefundResponse completeRequestedRefund(Long refundId) {
        Refund refund = refundService.findById(refundId);

        // REQUESTED 상태가 아닌 환불은 외부 PG 성공 이후의 완료 처리 대상으로 볼 수 없으므로 차단한다.
        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.INVALID_REFUND_STATUS);
        }

        Payment payment = refund.getPayment();
        log.info("환불 완료 내부 처리 시작: orderId={}, paymentId={}, refundId={}",
                payment.getOrder().getId(), payment.getId(), refund.getId());

        // PortOne 환불이 성공했으므로 환불 요청 상태를 COMPLETED로 확정한다.
        refund.complete();

        // 환불된 주문 상품 수량만큼 재고를 복구한다.
        List<RefundItem> refundItems = refundService.findItemsByRefundId(refundId);
        for (RefundItem refundItem : refundItems) {
            restoreStock(refundItem.getOrderItem(), refundItem.getQuantity());
        }

        // 주문 시 사용했던 포인트 중 이번 환불 비율에 해당하는 금액을 회원에게 복구한다.
        restoreUsedPoint(payment, refund.getPointRefundAmount());

        // 결제 적립 포인트를 회수하되, 잔액 부족분은 이미 PG 환불액에서 차감된 금액만큼 제외한다.
        cancelEarnedPoint(
                payment,
                refund.getEarnedPointCancelAmount(),
                refund.getEarnedPointDeductionAmount()
        );

        // 누적 완료 환불 금액을 기준으로 결제 상태를 PARTIAL_REFUNDED 또는 REFUNDED로 전이한다.
        updatePaymentAndOrderStatus(payment);

        log.info("환불 완료 내부 처리 완료: orderId={}, paymentId={}, refundId={}, totalRefundAmount={}, paymentStatus={}",
                payment.getOrder().getId(), payment.getId(), refund.getId(),
                refund.getTotalRefundAmount(), payment.getStatus());

        return RefundResponse.of(
                refund.getId(),
                payment.getId(),
                refund.getStatus(),
                refund.getTotalRefundAmount(),
                refund.getPointRefundAmount(),
                refund.getPgRefundAmount(),
                refund.getEarnedPointCancelAmount(),
                refund.getEarnedPointDeductionAmount(),
                payment.getStatus()
        );
    }

    /**
     * REQUESTED 환불을 실패 상태로 변경한다.
     *
     * <p>FAILED 환불은 환불 가능 수량 계산에서 제외되므로, PortOne 환불 실패 후 같은 상품 수량을
     * 다시 환불 요청할 수 있다.</p>
     *
     * @param refundId 환불 ID
     */
    @Transactional
    public void failRequestedRefund(Long refundId) {
        Refund refund = refundService.findById(refundId);
        if (refund.getStatus() == RefundStatus.COMPLETED) {
            return;
        }

        refund.fail();
        log.warn("환불 요청 실패 처리 완료: orderId={}, paymentId={}, refundId={}",
                refund.getPayment().getOrder().getId(), refund.getPayment().getId(), refundId);
    }

    private RefundAmount calculateRefundAmount(Payment payment, List<RefundItemRequest> items) {
        Long totalRefundAmount = 0L;
        for (RefundItemRequest item : items) {
            OrderItem orderItem = findOrderItemForRefund(payment, item.orderItemId());

            // REQUESTED와 COMPLETED 환불 수량을 포함해 남은 환불 가능 수량을 초과하는지 검증한다.
            refundService.validateRefundQuantity(orderItem, item.quantity());

            // 현재 상품 가격이 아닌 주문 당시 가격 스냅샷으로 환불 금액을 계산한다.
            totalRefundAmount += calculateItemRefundAmount(orderItem, item.quantity());
        }

        if (totalRefundAmount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REFUND_QUANTITY);
        }

        // PG 환불액은 결제 비율에 따라 계산하되 소수점이 발생하면 원 단위 올림한다.
        Long basePgRefundAmount = calculatePgRefundAmountByCeil(
                totalRefundAmount,
                payment.getPgAmount(),
                payment.getTotalAmount()
        );

        // 사용 포인트 복구액은 전체 환불액에서 PG 환불액을 뺀 값으로 맞춰 합계 오차를 없앤다.
        Long pointRefundAmount = totalRefundAmount - basePgRefundAmount;

        // 누적 환불 금액 기준으로 이번 환불에서 회수해야 할 적립 포인트 차액을 계산한다.
        Long earnedPointCancelAmount = calculateCurrentEarnedPointCancelAmount(payment, totalRefundAmount);

        // 사용 포인트를 먼저 복구한다고 가정한 뒤, 그 시점의 가용 포인트로 적립 포인트 회수 가능 여부를 판단한다.
        Long availablePointAfterUsePointRefund = (long) payment.getOrder().getMember().getPointBalance()
                + pointRefundAmount;

        // 적립 포인트 회수 부족분은 PG 환불액에서 차감한다.
        Long earnedPointDeductionAmount = calculateEarnedPointDeductionAmount(
                earnedPointCancelAmount,
                availablePointAfterUsePointRefund,
                basePgRefundAmount
        );

        // PortOne에는 적립 포인트 부족분이 차감된 최종 PG 환불액만 요청한다.
        Long pgRefundAmount = basePgRefundAmount - earnedPointDeductionAmount;

        return new RefundAmount(
                totalRefundAmount,
                pointRefundAmount,
                pgRefundAmount,
                earnedPointCancelAmount,
                earnedPointDeductionAmount
        );
    }

    private OrderItem findOrderItemForRefund(Payment payment, Long orderItemId) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        // 요청한 주문 상품이 해당 결제의 주문에 속하는지 검증해 다른 주문상품 환불을 차단한다.
        if (!orderItem.getOrder().getId().equals(payment.getOrder().getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return orderItem;
    }

    private void validateOwnership(Payment payment, Long memberId) {
        Long ownerId = payment.getOrder().getMember().getId();

        // JWT 인증 사용자와 결제 주문의 소유자가 같은지 검증한다.
        if (!Objects.equals(ownerId, memberId)) {
            log.warn("환불 결제 소유권 검증 실패: memberId={}, ownerId={}, orderId={}, paymentId={}",
                    memberId, ownerId, payment.getOrder().getId(), payment.getId());
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateDuplicateOrderItems(List<RefundItemRequest> items) {
        Set<Long> orderItemIds = new HashSet<>();
        for (RefundItemRequest item : items) {
            // 같은 주문 상품이 요청 안에 중복되면 수량을 합산하지 않고 잘못된 요청으로 처리한다.
            if (!orderItemIds.add(item.orderItemId())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
        }
    }

    private Long calculateItemRefundAmount(OrderItem orderItem, Integer quantity) {
        return (long) orderItem.getProductPriceSnapshot() * quantity;
    }

    private Long calculateProportionalAmount(Long requestAmount, Long targetAmount, Long totalAmount) {
        if (targetAmount <= 0 || totalAmount <= 0) {
            return 0L;
        }
        return requestAmount * targetAmount / totalAmount;
    }

    private Long calculatePgRefundAmountByCeil(Long refundAmount, Long pgAmount, Long totalAmount) {
        if (pgAmount <= 0 || totalAmount <= 0) {
            return 0L;
        }

        // 정수 나눗셈에서 (분자 + 분모 - 1) / 분모 공식을 사용해 원 단위 올림을 구현한다.
        return (refundAmount * pgAmount + totalAmount - 1) / totalAmount;
    }

    private void restoreStock(OrderItem orderItem, int quantity) {
        // 환불 완료 후 재고 복구 정합성을 위해 상품 row에 비관적 락을 건다.
        Product product = productRepository.findByIdWithLock(orderItem.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        product.increaseStock(quantity);
        log.info("환불 재고 복구 완료: orderItemId={}, productId={}, quantity={}",
                orderItem.getId(), product.getId(), quantity);
    }

    private void restoreUsedPoint(Payment payment, Long pointRefundAmount) {
        if (pointRefundAmount <= 0) {
            return;
        }

        // 주문 때 사용했던 포인트 중 이번 환불에 해당하는 금액을 회원 잔액에 복구한다.
        payment.getOrder().getMember().restoreUsePoint(pointRefundAmount.intValue());
        log.info("환불 사용 포인트 복구 완료: orderId={}, paymentId={}, pointRefundAmount={}",
                payment.getOrder().getId(), payment.getId(), pointRefundAmount);
    }

    private void cancelEarnedPoint(
            Payment payment,
            Long earnedPointCancelAmount,
            Long earnedPointDeductionAmount
    ) {
        Long pointCancelAmount = earnedPointCancelAmount - earnedPointDeductionAmount;
        if (pointCancelAmount <= 0) {
            return;
        }

        // 포인트 잔액으로 회수 가능한 적립 포인트만 차감한다. 부족분은 PG 환불액에서 이미 차감했다.
        payment.getOrder().getMember().cancelEarnPoint(pointCancelAmount.intValue());
        log.info("환불 적립 포인트 회수 완료: orderId={}, paymentId={}, pointCancelAmount={}, deductionAmount={}",
                payment.getOrder().getId(), payment.getId(), pointCancelAmount, earnedPointDeductionAmount);
    }

    private Long calculateCurrentEarnedPointCancelAmount(Payment payment, Long currentRefundAmount) {
        Long earnedPointAmount = payment.getEarnedPointAmount();
        if (earnedPointAmount <= 0) {
            return 0L;
        }

        // 이전 누적 환불액과 이번 환불 후 누적 환불액의 차이로 이번에 추가 회수할 적립 포인트를 계산한다.
        Long completedRefundAmount = refundService.calculateCompletedTotalRefundAmount(payment.getId());
        Long previousRefundAmount = completedRefundAmount;
        Long currentCompletedRefundAmount = completedRefundAmount + currentRefundAmount;
        Long previousCancelAmount = calculateEarnedPointCancelAmount(
                previousRefundAmount,
                earnedPointAmount,
                payment.getTotalAmount()
        );
        Long currentCancelAmount = calculateEarnedPointCancelAmount(
                currentCompletedRefundAmount,
                earnedPointAmount,
                payment.getTotalAmount()
        );
        return currentCancelAmount - previousCancelAmount;
    }

    private Long calculateEarnedPointDeductionAmount(
            Long earnedPointCancelAmount,
            Long availablePointAmount,
            Long basePgRefundAmount
    ) {
        Long shortageAmount = earnedPointCancelAmount - availablePointAmount;
        if (shortageAmount <= 0) {
            return 0L;
        }

        // 적립 포인트 부족분이 PG 환불액보다 크더라도 실제 PG 환불액 이하만 차감할 수 있다.
        return Math.min(shortageAmount, basePgRefundAmount);
    }

    private Long calculateEarnedPointCancelAmount(
            Long completedRefundAmount,
            Long earnedPointAmount,
            Long totalAmount
    ) {
        if (completedRefundAmount >= totalAmount) {
            return earnedPointAmount;
        }

        return calculateProportionalAmount(
                completedRefundAmount,
                earnedPointAmount,
                totalAmount
        );
    }

    private void updatePaymentAndOrderStatus(Payment payment) {
        Long completedRefundAmount = refundService.calculateCompletedTotalRefundAmount(payment.getId());

        // 누적 완료 환불 금액이 결제 총액 이상이면 전액 환불로 보고 주문도 취소 상태로 전이한다.
        if (completedRefundAmount >= payment.getTotalAmount()) {
            payment.markRefunded();
            payment.getOrder().cancelByRefund();
            return;
        }

        // 아직 환불 가능 금액이 남아 있으면 부분 환불 상태로 전이한다.
        payment.markPartialRefunded();
    }

    /**
     * PortOne 환불 요청에 필요한 REQUESTED 환불 정보이다.
     *
     * @param refundId 환불 ID
     * @param portonePaymentId PortOne 결제 식별자
     * @param pgRefundAmount PG 환불 요청 금액
     * @param idempotencyKey PortOne 멱등성 키
     */
    public record RequestedRefundResult(
            Long refundId,
            String portonePaymentId,
            Long pgRefundAmount,
            String idempotencyKey
    ) {
    }

    private record RefundAmount(
            Long totalRefundAmount,
            Long pointRefundAmount,
            Long pgRefundAmount,
            Long earnedPointCancelAmount,
            Long earnedPointDeductionAmount
    ) {
    }
}
