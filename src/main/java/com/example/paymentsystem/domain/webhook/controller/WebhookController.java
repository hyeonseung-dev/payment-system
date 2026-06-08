package com.example.paymentsystem.domain.webhook.controller;

import com.example.paymentsystem.global.response.ApiResponse;
import com.example.paymentsystem.infra.portone.webhook.PortOneWebhookHandler;
import com.example.paymentsystem.infra.portone.webhook.PortOneWebhookVerifier;
import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortOne 웹훅 요청을 수신하는 컨트롤러이다.
 *
 * <p>웹훅은 JWT 인증 대상이 아니므로 PortOne Standard Webhooks 헤더를 이용해 서명을 검증한다.
 * 서명 검증에 실패한 요청은 결제 도메인 로직으로 전달하지 않고 로그만 남긴 뒤 종료한다.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final PortOneWebhookVerifier portOneWebhookVerifier;
    private final PortOneWebhookHandler portOneWebhookHandler;

    /**
     * PortOne 웹훅을 수신한다.
     *
     * @param webhookId 웹훅 식별자
     * @param webhookTimestamp 웹훅 생성 시각
     * @param webhookSignature 웹훅 서명
     * @param body 웹훅 요청 본문 원문
     * @return 웹훅 수신 응답
     */
    @PostMapping("/portone")
    public ResponseEntity<ApiResponse<Void>> handlePortOneWebhook(
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-timestamp") String webhookTimestamp,
            @RequestHeader("webhook-signature") String webhookSignature,
            @RequestBody String body
    ) {
        log.info("PortOne 웹훅 수신: webhookId={}, webhookTimestamp={}", webhookId, webhookTimestamp);

        Webhook webhook;
        try {
            webhook = portOneWebhookVerifier.verify(
                    body,
                    webhookId,
                    webhookSignature,
                    webhookTimestamp
            );
        } catch (WebhookVerificationException e) {
            log.warn("PortOne 웹훅 서명 검증 실패: webhookId={}, reason={}", webhookId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.ok());
        }

        portOneWebhookHandler.handle(webhookId, webhook, body);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
