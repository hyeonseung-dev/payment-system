package com.example.paymentsystem.domain.refund.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 환불 요청에 포함되는 주문 상품별 환불 수량 DTO이다.
 *
 * @param orderItemId 환불할 주문 상품 ID
 * @param quantity 환불 수량
 */
public record RefundItemRequest(
        @NotNull(message = "주문 상품 ID는 필수입니다.")
        Long orderItemId,
        @NotNull(message = "환불 수량은 필수입니다.")
        @Min(value = 1, message = "환불 수량은 1 이상이어야 합니다.")
        Integer quantity
) {
}
