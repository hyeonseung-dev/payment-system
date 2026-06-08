package com.example.paymentsystem.domain.webhook.service;

import com.example.paymentsystem.domain.webhook.entity.WebhookEvent;
import com.example.paymentsystem.domain.webhook.entity.WebhookStatus;
import com.example.paymentsystem.domain.webhook.repository.WebhookEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 웹훅 이벤트 이력의 멱등 저장과 상태 변경을 검증한다.
 *
 * <p>PortOne은 같은 웹훅을 재전송할 수 있으므로 webhookId 기준 중복 저장을 막아야 한다.
 * 또한 처리 결과가 COMPLETED, FAILED, IGNORED로 명확히 남아야 운영 추적이 가능하다.</p>
 */
@ExtendWith(MockitoExtension.class)
class WebhookEventServiceTest {

    @InjectMocks
    private WebhookEventService webhookEventService;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Test
    void 새_webhookId는_RECEIVED_상태로_저장한다() {
        // given
        WebhookEvent savedEvent = WebhookEvent.receive(
                null,
                "msg-1",
                "pay-1",
                "Transaction.Paid",
                "{}"
        );

        when(webhookEventRepository.existsByWebhookId("msg-1")).thenReturn(false);
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenReturn(savedEvent);

        // when
        Optional<WebhookEvent> result = webhookEventService.createReceivedEventIfNotDuplicate(
                null,
                "msg-1",
                "pay-1",
                "Transaction.Paid",
                "{}"
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(WebhookStatus.RECEIVED);
        verify(webhookEventRepository).save(any(WebhookEvent.class));
    }

    @Test
    void 이미_존재하는_webhookId는_새로_저장하지_않는다() {
        // given
        when(webhookEventRepository.existsByWebhookId("msg-1")).thenReturn(true);

        // when
        Optional<WebhookEvent> result = webhookEventService.createReceivedEventIfNotDuplicate(
                null,
                "msg-1",
                "pay-1",
                "Transaction.Paid",
                "{}"
        );

        // then
        assertThat(result).isEmpty();
        verify(webhookEventRepository, never()).save(any(WebhookEvent.class));
    }

    @Test
    void 처리결과에_따라_COMPLETED_FAILED_IGNORED_상태를_기록한다() {
        // given
        WebhookEvent event = WebhookEvent.receive(null, "msg-1", "pay-1", "Transaction.Paid", "{}");

        when(webhookEventRepository.findById(1L)).thenReturn(Optional.of(event));

        // when & then
        webhookEventService.completeEvent(1L);
        assertThat(event.getStatus()).isEqualTo(WebhookStatus.COMPLETED);
        assertThat(event.getFailureReason()).isNull();

        webhookEventService.failEvent(1L, "PortOne 조회 실패");
        assertThat(event.getStatus()).isEqualTo(WebhookStatus.FAILED);
        assertThat(event.getFailureReason()).isEqualTo("PortOne 조회 실패");

        webhookEventService.ignoreEvent(1L, "처리 대상이 아닌 이벤트");
        assertThat(event.getStatus()).isEqualTo(WebhookStatus.IGNORED);
        assertThat(event.getFailureReason()).isEqualTo("처리 대상이 아닌 이벤트");
    }
}
