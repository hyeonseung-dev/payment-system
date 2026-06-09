package com.example.paymentsystem.infra.portone.dto;

/**
 * PortOne 결제 조회 응답 DTO이다.
 *
 * @param portonePaymentId PortOne 결제 식별자
 * @param status PortOne 결제 상태
 * @param paidAmount PortOne 승인 결제 금액
 */
public record PortOnePaymentResponse(
        String portonePaymentId,
        String status,
        Long paidAmount
) {
}
