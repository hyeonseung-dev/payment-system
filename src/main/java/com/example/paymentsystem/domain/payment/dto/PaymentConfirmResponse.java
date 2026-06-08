package com.example.paymentsystem.domain.payment.dto;

import com.example.paymentsystem.domain.payment.entity.Payment;

/**
 * 결제 확정 응답 DTO이다.
 *
 * <p>결제 확정 처리가 끝난 뒤 클라이언트에 전달할 결제 결과 정보를 담는다.
 * Controller는 Entity를 직접 반환하지 않고 이 DTO로 변환해서 응답한다.</p>
 *
 * @param orderId 주문 ID
 * @param paymentId 결제 ID
 * @param orderStatus 주문 상태
 * @param paymentStatus 결제 상태
 * @param totalAmount 주문 총 금액
 * @param pointAmount 사용 포인트 금액
 * @param pgAmount PG 결제 금액
 * @param earnedPointAmount 적립 예정 포인트
 */
public record PaymentConfirmResponse(
        Long orderId,
        Long paymentId,
        String orderStatus,
        String paymentStatus,
        Long totalAmount,
        int pointAmount,
        Long pgAmount,
        Long earnedPointAmount
) {

    /**
     * Payment 엔티티를 결제 확정 응답 DTO로 변환한다.
     *
     * @param payment 결제 엔티티
     * @return 결제 확정 응답 DTO
     */
    public static PaymentConfirmResponse from(Payment payment) {
        return new PaymentConfirmResponse(
                payment.getOrder().getId(),
                payment.getId(),
                payment.getOrder().getStatus().name(),
                payment.getStatus().name(),
                payment.getTotalAmount(),
                payment.getOrder().getUsePointAmountSnapshot(),
                payment.getPgAmount(),
                payment.getEarnedPointAmount()
        );
    }
}
