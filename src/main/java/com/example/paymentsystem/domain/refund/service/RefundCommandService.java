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
        Payment payment = paymentService.findByIdWithOrderAndMemberForUpdate(paymentId);
        log.info("환불 요청 선점 시작: memberId={}, orderId={}, paymentId={}, paymentStatus={}",
                memberId, payment.getOrder().getId(), paymentId, payment.getStatus());

        validateOwnership(payment, memberId);
        refundService.validateRefundablePayment(payment);
        validateDuplicateOrderItems(items);

        RefundAmount refundAmount = calculateRefundAmount(payment, items);
        Refund refund = refundService.createRequestedRefund(
                payment,
                reason,
                refundAmount.totalRefundAmount(),
                refundAmount.pointRefundAmount(),
                refundAmount.pgRefundAmount(),
                refundAmount.earnedPointCancelAmount(),
                refundAmount.earnedPointDeductionAmount()
        );

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

            remainingPointRefundAmount -= itemPointRefundAmount;
            remainingPgRefundAmount -= itemPgRefundAmount;
        }

        log.info("환불 요청 선점 완료: orderId={}, paymentId={}, refundId={}, pgRefundAmount={}",
                payment.getOrder().getId(), paymentId, refund.getId(), refundAmount.pgRefundAmount());

        return new RequestedRefundResult(
                refund.getId(),
                payment.getPortonePaymentId(),
                refundAmount.pgRefundAmount()
        );
    }

    /**
     * REQUESTED 환불을 완료 처리하고 재고, 포인트, 결제 상태를 반영한다.
     *
     * @param refundId 환불 ID
     * @return 환불 응답
     */
    @Transactional
    public RefundResponse completeRequestedRefund(Long refundId) {
        Refund refund = refundService.findById(refundId);
        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.INVALID_REFUND_STATUS);
        }

        Payment payment = refund.getPayment();
        log.info("환불 완료 내부 처리 시작: orderId={}, paymentId={}, refundId={}",
                payment.getOrder().getId(), payment.getId(), refund.getId());

        refund.complete();

        List<RefundItem> refundItems = refundService.findItemsByRefundId(refundId);
        for (RefundItem refundItem : refundItems) {
            restoreStock(refundItem.getOrderItem(), refundItem.getQuantity());
        }

        restoreUsedPoint(payment, refund.getPointRefundAmount());
        cancelEarnedPoint(
                payment,
                refund.getEarnedPointCancelAmount(),
                refund.getEarnedPointDeductionAmount()
        );
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
            refundService.validateRefundQuantity(orderItem, item.quantity());
            totalRefundAmount += calculateItemRefundAmount(orderItem, item.quantity());
        }

        if (totalRefundAmount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REFUND_QUANTITY);
        }

        Long basePgRefundAmount = calculatePgRefundAmountByCeil(
                totalRefundAmount,
                payment.getPgAmount(),
                payment.getTotalAmount()
        );
        Long pointRefundAmount = totalRefundAmount - basePgRefundAmount;
        Long earnedPointCancelAmount = calculateCurrentEarnedPointCancelAmount(payment, totalRefundAmount);
        Long availablePointAfterUsePointRefund = (long) payment.getOrder().getMember().getPointBalance()
                + pointRefundAmount;
        Long earnedPointDeductionAmount = calculateEarnedPointDeductionAmount(
                earnedPointCancelAmount,
                availablePointAfterUsePointRefund,
                basePgRefundAmount
        );
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

        if (!orderItem.getOrder().getId().equals(payment.getOrder().getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return orderItem;
    }

    private void validateOwnership(Payment payment, Long memberId) {
        Long ownerId = payment.getOrder().getMember().getId();
        if (!Objects.equals(ownerId, memberId)) {
            log.warn("환불 결제 소유권 검증 실패: memberId={}, ownerId={}, orderId={}, paymentId={}",
                    memberId, ownerId, payment.getOrder().getId(), payment.getId());
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateDuplicateOrderItems(List<RefundItemRequest> items) {
        Set<Long> orderItemIds = new HashSet<>();
        for (RefundItemRequest item : items) {
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
        return (refundAmount * pgAmount + totalAmount - 1) / totalAmount;
    }

    private void restoreStock(OrderItem orderItem, int quantity) {
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

        payment.getOrder().getMember().cancelEarnPoint(pointCancelAmount.intValue());
        log.info("환불 적립 포인트 회수 완료: orderId={}, paymentId={}, pointCancelAmount={}, deductionAmount={}",
                payment.getOrder().getId(), payment.getId(), pointCancelAmount, earnedPointDeductionAmount);
    }

    private Long calculateCurrentEarnedPointCancelAmount(Payment payment, Long currentRefundAmount) {
        Long earnedPointAmount = payment.getEarnedPointAmount();
        if (earnedPointAmount <= 0) {
            return 0L;
        }

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
        if (completedRefundAmount >= payment.getTotalAmount()) {
            payment.markRefunded();
            payment.getOrder().cancelByRefund();
            return;
        }

        payment.markPartialRefunded();
    }

    /**
     * PortOne 환불 요청에 필요한 REQUESTED 환불 정보이다.
     *
     * @param refundId 환불 ID
     * @param portonePaymentId PortOne 결제 식별자
     * @param pgRefundAmount PG 환불 요청 금액
     */
    public record RequestedRefundResult(
            Long refundId,
            String portonePaymentId,
            Long pgRefundAmount
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
