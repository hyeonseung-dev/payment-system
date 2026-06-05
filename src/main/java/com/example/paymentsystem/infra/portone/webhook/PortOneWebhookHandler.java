package com.example.paymentsystem.infra.portone.webhook;

import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.domain.payment.facade.PaymentFacade;
import com.example.paymentsystem.domain.payment.service.PaymentService;
import com.example.paymentsystem.domain.webhook.entity.WebhookEvent;
import com.example.paymentsystem.domain.webhook.service.WebhookEventService;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import io.portone.sdk.server.webhook.Webhook;
import io.portone.sdk.server.webhook.WebhookTransaction;
import io.portone.sdk.server.webhook.WebhookTransactionCancelledCancelled;
import io.portone.sdk.server.webhook.WebhookTransactionPaid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 검증된 PortOne 웹훅을 결제 도메인 흐름으로 연결하는 핸들러이다.
 *
 * <p>웹훅 이벤트의 종류를 판단하고 PortOne 결제 식별자를 추출한 뒤 웹훅 수신 이력을 저장한다.
 * 결제 완료 웹훅은 결제 확정 Facade로 전달하고, 현재 처리하지 않는 이벤트는 IGNORED 상태로 남긴다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortOneWebhookHandler {

    private static final String TRANSACTION_PAID_EVENT_TYPE = "Transaction.Paid";
    private static final String TRANSACTION_CANCELLED_EVENT_TYPE = "Transaction.Cancelled";
    private static final String UNSUPPORTED_EVENT_REASON_PREFIX = "처리 대상이 아닌 웹훅 이벤트: ";
    private static final int FAILURE_REASON_MAX_LENGTH = 1000;

    private final WebhookEventService webhookEventService;
    private final PaymentService paymentService;
    private final PaymentFacade paymentFacade;

    /**
     * PortOne 웹훅을 처리한다.
     *
     * @param webhookId 웹훅 식별자
     * @param webhook 검증된 PortOne 웹훅 객체
     * @param payload 웹훅 원문
     */
    public void handle(String webhookId, Webhook webhook, String payload) {
        String portonePaymentId = extractPortonePaymentId(webhook);
        String eventType = extractEventType(webhook);

        log.info("PortOne 웹훅 처리 시작: webhookId={}, eventType={}, portonePaymentId={}",
                webhookId, eventType, portonePaymentId);

        Payment payment = paymentService.findOptionalByPortonePaymentIdWithOrderAndMember(portonePaymentId)
                .orElse(null);

        Optional<WebhookEvent> savedEvent = webhookEventService.createReceivedEventIfNotDuplicate(
                payment,
                webhookId,
                portonePaymentId,
                eventType,
                payload
        );

        if (savedEvent.isEmpty()) {
            log.info("중복 PortOne 웹훅 무시: webhookId={}, eventType={}, portonePaymentId={}",
                    webhookId, eventType, portonePaymentId);
            return;
        }

        WebhookEvent webhookEvent = savedEvent.get();

        try {
            if (webhook instanceof WebhookTransactionPaid) {
                paymentFacade.confirmPaymentFromWebhook(portonePaymentId);
                webhookEventService.completeEvent(webhookEvent.getId());
                log.info("PortOne 결제 완료 웹훅 처리 완료: webhookId={}, eventId={}, portonePaymentId={}",
                        webhookId, webhookEvent.getId(), portonePaymentId);
                return;
            }

            if (webhook instanceof WebhookTransactionCancelledCancelled) {
                paymentFacade.cancelPaymentFromWebhook(portonePaymentId);
                webhookEventService.completeEvent(webhookEvent.getId());
                log.info("PortOne 결제취소 웹훅 처리 완료: webhookId={}, eventId={}, portonePaymentId={}",
                        webhookId, webhookEvent.getId(), portonePaymentId);
                return;
            }

            webhookEventService.ignoreEvent(
                    webhookEvent.getId(),
                    truncateFailureReason(UNSUPPORTED_EVENT_REASON_PREFIX + eventType)
            );
            log.info("처리 대상이 아닌 PortOne 웹훅 무시: webhookId={}, eventId={}, eventType={}, portonePaymentId={}",
                    webhookId, webhookEvent.getId(), eventType, portonePaymentId);
        } catch (Exception e) {
            webhookEventService.failEvent(webhookEvent.getId(), truncateFailureReason(e.getMessage()));
            log.warn("PortOne 웹훅 처리 실패: webhookId={}, eventId={}, eventType={}, portonePaymentId={}, reason={}",
                    webhookId, webhookEvent.getId(), eventType, portonePaymentId, e.getMessage());
        }
    }

    private String extractPortonePaymentId(Webhook webhook) {
        if (webhook instanceof WebhookTransaction transaction) {
            String paymentId = transaction.getData().getPaymentId();
            if (paymentId != null && !paymentId.isBlank()) {
                return paymentId;
            }
        }
        throw new BusinessException(ErrorCode.WEBHOOK_PAYMENT_ID_MISSING);
    }

    private String extractEventType(Webhook webhook) {
        if (webhook instanceof WebhookTransactionPaid) {
            return TRANSACTION_PAID_EVENT_TYPE;
        }
        if (webhook instanceof WebhookTransactionCancelledCancelled) {
            return TRANSACTION_CANCELLED_EVENT_TYPE;
        }
        return webhook.getClass().getSimpleName();
    }

    private String truncateFailureReason(String failureReason) {
        if (failureReason == null) {
            return "웹훅 처리 중 알 수 없는 오류가 발생했습니다.";
        }
        if (failureReason.length() <= FAILURE_REASON_MAX_LENGTH) {
            return failureReason;
        }
        return failureReason.substring(0, FAILURE_REASON_MAX_LENGTH);
    }
}
