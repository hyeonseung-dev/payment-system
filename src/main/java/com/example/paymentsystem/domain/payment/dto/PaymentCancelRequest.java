package com.example.paymentsystem.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 결제취소 요청 DTO이다.
 *
 * @param reason 결제취소 사유
 */
public record PaymentCancelRequest(
        @NotBlank(message = "결제취소 사유는 필수입니다.")
        String reason
) {
}
