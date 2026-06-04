package com.example.paymentsystem.domain.webhook.entity;

import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PortOne 웹훅 수신 내역과 처리 결과를 관리하는 엔티티이다.
 */
@Getter
@Entity
@Table(
        name = "webhook_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_webhook_events_webhook_id", columnNames = "webhook_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = true)
    private Payment payment;

    @Column(name = "webhook_id", nullable = false, length = 100)
    private String webhookId;

    @Column(name = "portone_payment_id", nullable = false, length = 100)
    private String portonePaymentId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WebhookStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(length = 1000)
    private String failureReason;

    private LocalDateTime processedAt;

    private WebhookEvent(
            Payment payment,
            String webhookId,
            String portonePaymentId,
            String eventType,
            String payload
    ) {
        this.payment = payment;
        this.webhookId = webhookId;
        this.portonePaymentId = portonePaymentId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = WebhookStatus.RECEIVED;
    }

    /**
     * 수신한 PortOne 웹훅 이벤트를 생성한다.
     *
     * <p>웹훅 수신 시점에는 결제 정보를 찾지 못할 수 있으므로 payment는 null일 수 있다.
     * 최초 상태는 RECEIVED로 저장한다.</p>
     *
     * @param payment 연결된 결제 정보
     * @param webhookId 웹훅 식별자
     * @param portonePaymentId PortOne 결제 식별자
     * @param eventType 웹훅 이벤트 종류
     * @param payload 웹훅 원문
     * @return 웹훅 이벤트 엔티티
     */
    public static WebhookEvent receive(
            Payment payment,
            String webhookId,
            String portonePaymentId,
            String eventType,
            String payload
    ) {
        return new WebhookEvent(payment, webhookId, portonePaymentId, eventType, payload);
    }

    /**
     * 웹훅 처리를 완료 상태로 변경한다.
     *
     * @param processedAt 처리 완료 시각
     */
    public void complete(LocalDateTime processedAt) {
        this.status = WebhookStatus.COMPLETED;
        this.processedAt = processedAt;
        this.failureReason = null;
    }

    /**
     * 웹훅 처리를 실패 상태로 변경한다.
     *
     * @param failureReason 실패 사유
     * @param processedAt 처리 실패 시각
     */
    public void fail(String failureReason, LocalDateTime processedAt) {
        this.status = WebhookStatus.FAILED;
        this.failureReason = failureReason;
        this.processedAt = processedAt;
    }

    /**
     * 웹훅을 무시 상태로 변경한다.
     *
     * @param processedAt 무시 처리 시각
     */
    public void ignore(LocalDateTime processedAt) {
        this.status = WebhookStatus.IGNORED;
        this.processedAt = processedAt;
        this.failureReason = null;
    }
}
