package com.example.paymentsystem.domain.payment.dto;

import com.example.paymentsystem.domain.payment.entity.Payment;

/**
 * 결제취소 응답 DTO이다.
 *
 * @param paymentId 결제 ID
 * @param orderId 주문 ID
 * @param portonePaymentId PortOne 결제 식별자
 * @param paymentStatus 결제 상태
 * @param orderStatus 주문 상태
 * @param message 응답 메시지
 */
public record PaymentCancelResponse(
        Long paymentId,
        Long orderId,
        String portonePaymentId,
        String paymentStatus,
        String orderStatus,
        String message
) {

    /**
     * Payment 엔티티를 결제취소 응답 DTO로 변환한다.
     *
     * @param payment 결제 엔티티
     * @param message 응답 메시지
     * @return 결제취소 응답 DTO
     */
    public static PaymentCancelResponse of(
            Payment payment,
            String message
    ) {
        return new PaymentCancelResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getPortonePaymentId(),
                payment.getStatus().name(),
                payment.getOrder().getStatus().name(),
                message
        );
    }
}
