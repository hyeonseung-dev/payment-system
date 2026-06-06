package com.example.paymentsystem.domain.refund.dto;

import com.example.paymentsystem.domain.payment.entity.PaymentStatus;
import com.example.paymentsystem.domain.refund.entity.RefundStatus;

/**
 * 전체 환불 응답 DTO이다.
 *
 * <p>전체 환불 처리 결과와 환불 이후 결제 상태를 클라이언트에 전달한다.</p>
 *
 * @param refundId 환불 ID
 * @param orderId 주문 ID
 * @param paymentId 결제 ID
 * @param refundStatus 환불 상태
 * @param paymentStatus 결제 상태
 * @param refundAmount 서버가 계산한 환불 금액
 * @param message 환불 처리 결과 메시지
 */
public record FullRefundResponse(
	Long refundId,
	Long orderId,
	Long paymentId,
	String refundStatus,
	String paymentStatus,
	Long refundAmount,
	String message
) {

	/**
	 * 전체 환불 처리 결과를 응답 DTO로 생성한다.
	 *
	 * @param refundId 환불 ID
	 * @param orderId 주문 ID
	 * @param paymentId 결제 ID
	 * @param refundStatus 환불 상태
	 * @param paymentStatus 결제 상태
	 * @param refundAmount 서버가 계산한 환불 금액
	 * @param message 환불 처리 결과 메시지
	 * @return 전체 환불 응답 DTO
	 */
	public static FullRefundResponse of(
		Long refundId,
		Long orderId,
		Long paymentId,
		RefundStatus refundStatus,
		PaymentStatus paymentStatus,
		Long refundAmount,
		String message
	) {
		return new FullRefundResponse(
			refundId,
			orderId,
			paymentId,
			refundStatus.name(),
			paymentStatus.name(),
			refundAmount,
			message
		);
	}
}
