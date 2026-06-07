package com.example.paymentsystem.domain.refund.service;

import com.example.paymentsystem.domain.order.entity.OrderItem;
import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.domain.payment.entity.PaymentStatus;
import com.example.paymentsystem.domain.refund.entity.Refund;
import com.example.paymentsystem.domain.refund.entity.RefundItem;
import com.example.paymentsystem.domain.refund.entity.RefundStatus;
import com.example.paymentsystem.domain.refund.repository.RefundItemRepository;
import com.example.paymentsystem.domain.refund.repository.RefundRepository;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 환불 정보 저장과 환불 가능 여부 검증을 담당하는 서비스이다.
 */
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final RefundItemRepository refundItemRepository;

    /**
     * 요청 상태의 환불 정보를 저장한다.
     *
     * @param payment 환불 대상 결제
     * @param reason 환불 사유
     * @param totalRefundAmount 전체 환불 금액
     * @param pointRefundAmount 포인트 환불 금액
     * @param pgRefundAmount PG 환불 금액
     * @param earnedPointCancelAmount 적립 포인트 회수 금액
     * @param earnedPointDeductionAmount 적립 포인트 부족으로 PG 환불액에서 차감한 금액
     * @param idempotencyKey PortOne 멱등성 키
     * @return 저장된 환불 정보
     */
    @Transactional
    public Refund createRequestedRefund(
            Payment payment,
            String reason,
            Long totalRefundAmount,
            Long pointRefundAmount,
            Long pgRefundAmount,
            Long earnedPointCancelAmount,
            Long earnedPointDeductionAmount,
            String idempotencyKey
    ) {
        Refund refund = Refund.createRequested(
                payment,
                reason,
                totalRefundAmount,
                pointRefundAmount,
                pgRefundAmount,
                earnedPointCancelAmount,
                earnedPointDeductionAmount,
                idempotencyKey
        );
        return refundRepository.save(refund);
    }

    /**
     * 실패 상태의 환불 정보를 저장한다.
     *
     * @param payment 환불 대상 결제
     * @param reason 환불 사유
     * @param totalRefundAmount 전체 환불 금액
     * @param pointRefundAmount 포인트 환불 금액
     * @param pgRefundAmount PG 환불 금액
     * @param earnedPointCancelAmount 적립 포인트 회수 금액
     * @param earnedPointDeductionAmount 적립 포인트 부족으로 PG 환불액에서 차감한 금액
     * @param idempotencyKey PortOne 멱등성 키
     * @return 저장된 환불 정보
     */
    @Transactional
    public Refund createFailedRefund(
            Payment payment,
            String reason,
            Long totalRefundAmount,
            Long pointRefundAmount,
            Long pgRefundAmount,
            Long earnedPointCancelAmount,
            Long earnedPointDeductionAmount,
            String idempotencyKey
    ) {
        Refund refund = Refund.createFailed(
                payment,
                reason,
                totalRefundAmount,
                pointRefundAmount,
                pgRefundAmount,
                earnedPointCancelAmount,
                earnedPointDeductionAmount,
                idempotencyKey
        );
        return refundRepository.save(refund);
    }

    /**
     * 환불 요청에 포함된 주문 상품별 환불 정보를 저장한다.
     *
     * @param refund 환불 요청
     * @param orderItem 환불 대상 주문 상품
     * @param quantity 환불 수량
     * @param pointRefundAmount 포인트 환불 금액
     * @param pgRefundAmount PG 환불 금액
     * @return 저장된 환불 상품 정보
     */
    @Transactional
    public RefundItem createRefundItem(
            Refund refund,
            OrderItem orderItem,
            Integer quantity,
            Long pointRefundAmount,
            Long pgRefundAmount
    ) {
        validateRefundQuantity(orderItem, quantity);

        RefundItem refundItem = RefundItem.create(
                refund,
                orderItem,
                quantity,
                pointRefundAmount,
                pgRefundAmount
        );
        return refundItemRepository.save(refundItem);
    }

    /**
     * 결제 ID로 환불 목록을 조회한다.
     *
     * @param paymentId 결제 ID
     * @return 환불 목록
     */
    @Transactional(readOnly = true)
    public List<Refund> findByPaymentId(Long paymentId) {
        return refundRepository.findByPaymentId(paymentId);
    }

    /**
     * 환불 ID로 환불 상품 목록을 조회한다.
     *
     * @param refundId 환불 ID
     * @return 환불 상품 목록
     */
    @Transactional(readOnly = true)
    public List<RefundItem> findItemsByRefundId(Long refundId) {
        return refundItemRepository.findByRefundId(refundId);
    }

    /**
     * 환불 ID로 환불 정보를 조회한다.
     *
     * @param refundId 환불 ID
     * @return 환불 정보
     */
    @Transactional(readOnly = true)
    public Refund findById(Long refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));
    }

    /**
     * 결제가 환불 가능한 상태인지 검증한다.
     *
     * @param payment 환불 대상 결제
     */
    public void validateRefundablePayment(Payment payment) {
        // 결제 완료 또는 부분 환불 상태인 결제만 추가 환불을 허용한다.
        if (payment.getStatus() != PaymentStatus.PAID
                && payment.getStatus() != PaymentStatus.PARTIAL_REFUNDED) {
            throw new BusinessException(ErrorCode.INVALID_REFUND_STATUS);
        }
    }

    /**
     * 주문 상품의 환불 요청 수량이 환불 가능 수량을 초과하지 않는지 검증한다.
     *
     * <p>REQUESTED 상태는 아직 PortOne 환불 완료 전이지만 중복 환불 요청을 막기 위해
     * 이미 선점된 수량으로 본다. FAILED 상태는 실패한 요청이므로 합산하지 않는다.</p>
     *
     * @param orderItem 환불 대상 주문 상품
     * @param refundQuantity 환불 요청 수량
     */
    public void validateRefundQuantity(OrderItem orderItem, Integer refundQuantity) {
        // 환불 수량은 1 이상이어야 한다.
        if (refundQuantity == null || refundQuantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REFUND_QUANTITY);
        }

        // REQUESTED + COMPLETED 수량을 선점된 수량으로 보고 남은 환불 가능 수량을 계산한다.
        int reservedRefundQuantity = calculateReservedRefundQuantity(orderItem.getId());
        int refundableQuantity = orderItem.getQuantity() - reservedRefundQuantity;

        // 요청 수량이 남은 환불 가능 수량보다 많으면 초과 환불이므로 차단한다.
        if (refundQuantity > refundableQuantity) {
            throw new BusinessException(ErrorCode.REFUND_QUANTITY_EXCEEDED);
        }
    }

    /**
     * 주문 상품 ID 기준으로 선점된 환불 수량 합계를 계산한다.
     *
     * @param orderItemId 주문 상품 ID
     * @return 선점된 환불 수량 합계
     */
    @Transactional(readOnly = true)
    public int calculateReservedRefundQuantity(Long orderItemId) {
        // FAILED 환불은 실제 환불이 완료되지 않았으므로 수량 선점 합계에서 제외한다.
        Long refundedQuantity = refundItemRepository.sumQuantityByOrderItemIdAndRefundStatuses(
                orderItemId,
                List.of(RefundStatus.REQUESTED, RefundStatus.COMPLETED)
        );
        return refundedQuantity.intValue();
    }

    /**
     * 결제 ID 기준 완료 환불의 총 환불 금액 합계를 계산한다.
     *
     * @param paymentId 결제 ID
     * @return 총 환불 금액 합계
     */
    @Transactional(readOnly = true)
    public Long calculateCompletedTotalRefundAmount(Long paymentId) {
        return refundRepository.sumTotalRefundAmountByPaymentIdAndStatus(
                paymentId,
                RefundStatus.COMPLETED
        );
    }
}
