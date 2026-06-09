package com.example.paymentsystem.infra.portone.client;

import com.example.paymentsystem.infra.portone.dto.PortOneCancelResponse;
import com.example.paymentsystem.infra.portone.dto.PortOnePaymentResponse;

/**
 * PortOne 결제 API 연동을 담당하는 클라이언트 인터페이스이다.
 */
public interface PortOneClient {

    /**
     * PortOne 결제 식별자로 결제 정보를 조회한다.
     *
     * @param portonePaymentId PortOne 결제 식별자
     * @return PortOne 결제 조회 응답
     */
    PortOnePaymentResponse getPayment(String portonePaymentId);

    /**
     * PortOne 결제 취소 또는 환불을 요청한다.
     *
     * @param portonePaymentId PortOne 결제 식별자
     * @param cancelAmount 취소 또는 환불 요청 금액
     * @param reason 취소 또는 환불 사유
     * @param idempotencyKey PortOne 멱등성 키
     * @return PortOne 결제 취소 응답
     */
    PortOneCancelResponse cancelPayment(
            String portonePaymentId,
            Long cancelAmount,
            String reason,
            String idempotencyKey
    );
}
