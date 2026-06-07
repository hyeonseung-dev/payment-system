package com.example.paymentsystem.infra.portone.client;

import com.example.paymentsystem.infra.portone.config.PortOneProperties;
import com.example.paymentsystem.infra.portone.dto.PortOneCancelRequest;
import com.example.paymentsystem.infra.portone.dto.PortOneCancelResponse;
import com.example.paymentsystem.infra.portone.dto.PortOnePaymentResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * RestClient를 사용해 PortOne 결제 API를 호출하는 구현체이다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PortOneClientImpl implements PortOneClient {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final int MAX_CANCEL_RETRY_COUNT = 3;

    private final RestClient portOneRestClient;
    private final PortOneProperties portOneProperties;

    /**
     * PortOne 결제 식별자로 결제 정보를 조회한다.
     *
     * @param portonePaymentId PortOne 결제 식별자
     * @return PortOne 결제 조회 응답
     */
    @Override
    public PortOnePaymentResponse getPayment(String portonePaymentId) {
        log.info("PortOne 결제 조회: {}", portonePaymentId);

        PortOnePaymentApiResponse response = portOneRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/payments/{paymentId}")
                        .queryParam("storeId", portOneProperties.storeId())
                        .build(portonePaymentId))
                .retrieve()
                .body(PortOnePaymentApiResponse.class);

        return new PortOnePaymentResponse(
                response.id(),
                response.status(),
                response.amount().total()
        );
    }

    /**
     * PortOne 결제 취소 또는 환불을 요청한다.
     *
     * @param portonePaymentId PortOne 결제 식별자
     * @param cancelAmount 취소 또는 환불 요청 금액
     * @param reason 취소 또는 환불 사유
     * @param idempotencyKey PortOne 멱등성 키
     * @return PortOne 결제 취소 응답
     */
    @Override
    public PortOneCancelResponse cancelPayment(
            String portonePaymentId,
            Long cancelAmount,
            String reason,
            String idempotencyKey
    ) {
        log.info("PortOne 결제 취소 요청: paymentId={}, amount={}, reason={}, idempotencyKey={}",
                portonePaymentId, cancelAmount, reason, idempotencyKey);

        PortOneCancelRequest request = new PortOneCancelRequest(cancelAmount, reason, portOneProperties.storeId());
        for (int attempt = 1; attempt <= MAX_CANCEL_RETRY_COUNT; attempt++) {
            try {
                PortOneCancelApiResponse response = portOneRestClient.post()
                        .uri("/payments/{paymentId}/cancel", portonePaymentId)
                        .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                        .body(request)
                        .retrieve()
                        .body(PortOneCancelApiResponse.class);

                return new PortOneCancelResponse(
                        portonePaymentId,
                        response.cancellation().totalAmount(),
                        response.cancellation().status()
                );
            } catch (ResourceAccessException e) {
                log.warn("PortOne 결제 취소 요청 네트워크 오류: paymentId={}, attempt={}/{}, idempotencyKey={}",
                        portonePaymentId, attempt, MAX_CANCEL_RETRY_COUNT, idempotencyKey);
                if (attempt == MAX_CANCEL_RETRY_COUNT) {
                    throw e;
                }
            }
        }

        throw new IllegalStateException("PortOne 결제 취소 요청 재시도 처리가 종료되지 않았습니다.");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PortOnePaymentApiResponse(
            String id,
            String status,
            PortOneAmount amount
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PortOneAmount(
            Long total
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PortOneCancelApiResponse(
            PortOneCancellation cancellation
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PortOneCancellation(
            String id,
            String status,
            Long totalAmount
    ) {
    }
}
