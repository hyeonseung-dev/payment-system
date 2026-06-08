package com.example.paymentsystem.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 결제 확정 요청 DTO이다.
 *
 * <p>클라이언트가 PortOne 결제창에서 결제를 완료한 뒤,
 * 서버에 결제 확정을 요청할 때 사용하는 입력값을 담는다.
 * 서버는 이 요청값을 기준으로 DB에 저장된 결제 정보와 PortOne 결제 정보를 비교한다.</p>
 *
 * @param orderId 결제를 확정할 주문 ID
 * @param portonePaymentId PortOne 결제 식별자
 */
public record PaymentConfirmRequest(
        @NotNull(message = "주문 ID는 필수입니다.")
        Long orderId,

        @NotBlank(message = "PortOne 결제 식별자는 필수입니다.")
        String portonePaymentId
) {
}
