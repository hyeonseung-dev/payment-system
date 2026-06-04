package com.example.paymentsystem.domain.webhook.entity;

/**
 * 웹훅 처리 상태를 나타낸다.
 *
 * <p>WebhookEvent는 PortOne에서 수신한 웹훅 1건의 처리 결과를 저장한다.</p>
 *
 * <ul>
 *     <li>RECEIVED: 웹훅을 수신하고 저장한 상태</li>
 *     <li>COMPLETED: 웹훅 처리가 정상적으로 완료된 상태</li>
 *     <li>FAILED: 웹훅 처리 중 오류가 발생한 상태</li>
 *     <li>IGNORED: 중복 웹훅 등 처리 대상이 아니어서 무시된 상태</li>
 * </ul>
 */
public enum WebhookStatus {

    RECEIVED,
    COMPLETED,
    FAILED,
    IGNORED
}
