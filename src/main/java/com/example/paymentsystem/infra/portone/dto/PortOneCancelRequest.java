package com.example.paymentsystem.infra.portone.dto;

/**
 * PortOne 결제 취소 요청 DTO이다.
 *
 * @param amount 취소 또는 환불 요청 금액
 * @param reason 취소 또는 환불 사유
 * @param storeId PortOne 상점 식별자
 */
public record PortOneCancelRequest(
        Long amount,
        String reason,
        String storeId
) {
}
