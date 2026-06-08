package com.example.paymentsystem.domain.order.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class OrderNumberGeneratorTest {

    private final OrderNumberGenerator orderNumberGenerator = new OrderNumberGenerator();

    @Test
    void 주문번호를_생성한다() {
        // when
        String orderNumber = orderNumberGenerator.generate();

        // then
        assertThat(orderNumber)
                .startsWith("ORD-")
                .matches("ORD-\\d{14}-[a-f0-9]{8}");
    }
}