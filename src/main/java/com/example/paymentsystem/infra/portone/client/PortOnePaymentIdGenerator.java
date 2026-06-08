package com.example.paymentsystem.infra.portone.client;

import java.util.UUID;

/**
 * PortOne 결제 식별자를 생성한다.
 */
public final class PortOnePaymentIdGenerator {

    private static final String PORTONE_PAYMENT_ID_PREFIX = "pay_";

    private PortOnePaymentIdGenerator() {
    }

    /**
     * PortOne 결제 식별자를 생성한다.
     *
     * @return pay_ 접두사를 가진 결제 식별자
     */
    public static String generatePortonePaymentId() {
        return PORTONE_PAYMENT_ID_PREFIX + UUID.randomUUID();
    }
}
