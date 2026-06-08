package com.example.paymentsystem.domain.refund.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 환불 요청 DTO이다.
 *
 * @param reason 환불 사유
 * @param items 환불할 주문 상품 목록
 */
public record RefundRequest(
        @NotBlank(message = "환불 사유는 필수입니다.")
        String reason,
        @Valid
        @NotEmpty(message = "환불할 주문 상품은 1개 이상이어야 합니다.")
        List<RefundItemRequest> items
) {
}
