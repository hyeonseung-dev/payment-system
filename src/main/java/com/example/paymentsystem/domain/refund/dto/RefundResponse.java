package com.example.paymentsystem.domain.refund.dto;

import com.example.paymentsystem.domain.payment.entity.PaymentStatus;
import com.example.paymentsystem.domain.refund.entity.RefundStatus;

/**
 * 환불 응답 DTO이다.
 *
 * @param refundId 환불 ID
 * @param paymentId 결제 ID
 * @param refundStatus 환불 상태
 * @param totalRefundAmount 서버가 계산한 전체 환불 금액
 * @param pointRefundAmount 포인트 환불 금액
 * @param pgRefundAmount PG 환불 금액
 * @param earnedPointCancelAmount 적립 포인트 회수 금액
 * @param earnedPointDeductionAmount 적립 포인트 부족으로 PG 환불액에서 차감한 금액
 * @param paymentStatus 환불 이후 결제 상태
 */
public record RefundResponse(
        Long refundId,
        Long paymentId,
        String refundStatus,
        Long totalRefundAmount,
        Long pointRefundAmount,
        Long pgRefundAmount,
        Long earnedPointCancelAmount,
        Long earnedPointDeductionAmount,
        String paymentStatus
) {

    /**
     * 환불 처리 결과를 응답 DTO로 생성한다.
     *
     * @param refundId 환불 ID
     * @param paymentId 결제 ID
     * @param refundStatus 환불 상태
     * @param totalRefundAmount 서버가 계산한 전체 환불 금액
     * @param pointRefundAmount 포인트 환불 금액
     * @param pgRefundAmount PG 환불 금액
     * @param earnedPointCancelAmount 적립 포인트 회수 금액
     * @param earnedPointDeductionAmount 적립 포인트 부족으로 PG 환불액에서 차감한 금액
     * @param paymentStatus 환불 이후 결제 상태
     * @return 환불 응답 DTO
     */
    public static RefundResponse of(
            Long refundId,
            Long paymentId,
            RefundStatus refundStatus,
            Long totalRefundAmount,
            Long pointRefundAmount,
            Long pgRefundAmount,
            Long earnedPointCancelAmount,
            Long earnedPointDeductionAmount,
            PaymentStatus paymentStatus
    ) {
        return new RefundResponse(
                refundId,
                paymentId,
                refundStatus.name(),
                totalRefundAmount,
                pointRefundAmount,
                pgRefundAmount,
                earnedPointCancelAmount,
                earnedPointDeductionAmount,
                paymentStatus.name()
        );
    }
}
