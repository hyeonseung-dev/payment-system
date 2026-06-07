package com.example.paymentsystem.infra.portone.client;

import java.util.UUID;

/**
 * PortOne 멱등성 키를 생성한다.
 */
public final class PortOneIdempotencyKeyGenerator {

    private PortOneIdempotencyKeyGenerator() {
    }

    /**
     * PortOne API 요청에 사용할 멱등성 키를 생성한다.
     *
     * @return UUID 기반 멱등성 키
     */
    public static String generateIdempotencyKey() {
        return UUID.randomUUID().toString();
    }
}
