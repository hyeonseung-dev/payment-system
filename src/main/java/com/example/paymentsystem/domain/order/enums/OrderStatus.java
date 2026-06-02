package com.example.paymentsystem.domain.order.enums;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    PAYMENT_PENDING("결제대기"),
    COMPLETED("주문완료"),
    CANCELED("주문 취소");

    private final String description;
}
