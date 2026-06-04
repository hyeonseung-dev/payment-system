package com.example.paymentsystem.infra.portone;

/**
 * PortOne 결제 취소 응답 DTO이다.
 *
 * @param portonePaymentId PortOne 결제 식별자
 * @param cancelledAmount 취소 또는 환불된 금액
 * @param status PortOne 결제 취소 상태
 */
public record PortOneCancelResponse(
        String portonePaymentId,
        Long cancelledAmount,
        String status
) {
}
