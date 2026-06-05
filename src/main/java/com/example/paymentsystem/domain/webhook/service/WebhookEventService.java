package com.example.paymentsystem.domain.webhook.service;

import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.domain.webhook.entity.WebhookEvent;
import com.example.paymentsystem.domain.webhook.repository.WebhookEventRepository;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * PortOne 웹훅 이벤트의 수신 이력과 처리 상태를 관리하는 서비스이다.
 *
 * <p>PortOne 웹훅은 네트워크 상황에 따라 같은 메시지가 여러 번 재전송될 수 있다.
 * 따라서 웹훅 식별자를 기준으로 이미 저장된 이벤트인지 확인하고, 신규 이벤트만
 * RECEIVED 상태로 저장한다. 이후 처리 결과에 따라 COMPLETED, FAILED, IGNORED 상태로 변경한다.</p>
 */
@Service
@RequiredArgsConstructor
public class WebhookEventService {

    private final WebhookEventRepository webhookEventRepository;

    /**
     * 웹훅 식별자가 중복되지 않은 경우 수신 이벤트를 저장한다.
     *
     * <p>중복 웹훅이면 새 이벤트를 만들지 않고 {@link Optional#empty()}를 반환한다.
     * 호출부는 이 값을 보고 이미 받은 웹훅을 다시 처리하지 않는다.</p>
     *
     * @param payment 연결된 결제 정보
     * @param webhookId 웹훅 식별자
     * @param portonePaymentId PortOne 결제 식별자
     * @param eventType 웹훅 이벤트 종류
     * @param payload 웹훅 원문
     * @return 신규 저장된 웹훅 이벤트
     */
    @Transactional
    public Optional<WebhookEvent> createReceivedEventIfNotDuplicate(
            Payment payment,
            String webhookId,
            String portonePaymentId,
            String eventType,
            String payload
    ) {
        if (webhookEventRepository.existsByWebhookId(webhookId)) {
            return Optional.empty();
        }

        WebhookEvent webhookEvent = WebhookEvent.receive(
                payment,
                webhookId,
                portonePaymentId,
                eventType,
                payload
        );
        return Optional.of(webhookEventRepository.save(webhookEvent));
    }

    /**
     * 웹훅 이벤트를 처리 완료 상태로 변경한다.
     *
     * @param eventId 웹훅 이벤트 ID
     */
    @Transactional
    public void completeEvent(Long eventId) {
        findById(eventId).complete(LocalDateTime.now());
    }

    /**
     * 웹훅 이벤트를 처리 실패 상태로 변경한다.
     *
     * @param eventId 웹훅 이벤트 ID
     * @param failureReason 실패 사유
     */
    @Transactional
    public void failEvent(Long eventId, String failureReason) {
        findById(eventId).fail(failureReason, LocalDateTime.now());
    }

    /**
     * 웹훅 이벤트를 무시 상태로 변경한다.
     *
     * @param eventId 웹훅 이벤트 ID
     */
    @Transactional
    public void ignoreEvent(Long eventId) {
        findById(eventId).ignore(LocalDateTime.now());
    }

    /**
     * 웹훅 이벤트 ID로 엔티티를 조회한다.
     *
     * @param eventId 웹훅 이벤트 ID
     * @return 웹훅 이벤트
     */
    @Transactional(readOnly = true)
    public WebhookEvent findById(Long eventId) {
        return webhookEventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WEBHOOK_EVENT_NOT_FOUND));
    }
}
