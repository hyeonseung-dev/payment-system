package com.example.paymentsystem.domain.webhook.repository;

import com.example.paymentsystem.domain.webhook.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 웹훅 이벤트 엔티티의 영속성 처리를 담당한다.
 */
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    /**
     * 웹훅 식별자로 웹훅 이벤트를 조회한다.
     *
     * @param webhookId 웹훅 식별자
     * @return 웹훅 이벤트
     */
    Optional<WebhookEvent> findByWebhookId(String webhookId);

    /**
     * 웹훅 식별자의 존재 여부를 확인한다.
     *
     * @param webhookId 웹훅 식별자
     * @return 존재하면 true
     */
    boolean existsByWebhookId(String webhookId);

    /**
     * PortOne 결제 식별자로 웹훅 이벤트 목록을 조회한다.
     *
     * @param portonePaymentId PortOne 결제 식별자
     * @return 해당 결제 식별자의 웹훅 이벤트 목록
     */
    List<WebhookEvent> findByPortonePaymentId(String portonePaymentId);

    /**
     * 결제 ID로 웹훅 이벤트 목록을 조회한다.
     *
     * @param paymentId 결제 ID
     * @return 해당 결제의 웹훅 이벤트 목록
     */
    List<WebhookEvent> findByPaymentId(Long paymentId);
}
