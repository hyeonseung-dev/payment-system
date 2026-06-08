package com.example.paymentsystem.infra.portone.webhook;

import com.example.paymentsystem.infra.portone.config.PortOneProperties;
import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.Webhook;
import io.portone.sdk.server.webhook.WebhookVerifier;
import org.springframework.stereotype.Component;

/**
 * PortOne 웹훅 메시지의 서명을 검증하는 컴포넌트이다.
 *
 * <p>웹훅은 JWT 인증 없이 외부에서 호출되므로 요청이 실제 PortOne에서 온 것인지
 * 확인하는 절차가 필요하다. PortOne 공식 Server SDK의 {@link WebhookVerifier}를 사용하면
 * Standard Webhooks 형식의 메시지 ID, 타임스탬프, 서명을 검증하고 검증된 웹훅 객체를 얻을 수 있다.</p>
 */
@Component
public class PortOneWebhookVerifier {

    private final WebhookVerifier webhookVerifier;

    /**
     * PortOne 웹훅 Secret으로 검증기를 생성한다.
     *
     * @param properties PortOne 연동 설정
     */
    public PortOneWebhookVerifier(PortOneProperties properties) {
        this.webhookVerifier = new WebhookVerifier(properties.webhookSecret());
    }

    /**
     * 웹훅 원문과 Standard Webhooks 헤더를 검증한다.
     *
     * @param body 웹훅 요청 본문 원문
     * @param webhookId 웹훅 메시지 식별자
     * @param signature 웹훅 서명
     * @param timestamp 웹훅 생성 시각
     * @return 검증된 PortOne 웹훅 객체
     * @throws WebhookVerificationException 서명, 타임스탬프, 메시지 형식 검증에 실패한 경우
     */
    public Webhook verify(
            String body,
            String webhookId,
            String signature,
            String timestamp
    ) throws WebhookVerificationException {
        return webhookVerifier.verify(body, webhookId, signature, timestamp);
    }
}
