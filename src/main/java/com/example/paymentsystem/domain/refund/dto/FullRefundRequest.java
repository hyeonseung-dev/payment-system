package com.example.paymentsystem.domain.refund.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 전체 환불 요청 DTO이다.
 *
 * @param orderId 전체 환불할 주문 ID
 * @param reason 환불 사유
 */
public record FullRefundRequest(
	@NotNull(message = "주문 ID는 필수입니다.")
	Long orderId,
	@NotBlank(message = "환불 사유는 필수입니다.")
	String reason
) {
}
