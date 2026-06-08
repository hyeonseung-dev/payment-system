package com.example.paymentsystem.domain.order.dto;

import com.example.paymentsystem.domain.order.entity.Order;
import com.example.paymentsystem.domain.order.enums.OrderStatus;
import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.domain.payment.entity.PaymentStatus;

public record OrderCancelResponse (
        Long orderId,
        String orderNumber,
        OrderStatus orderStatus,
        PaymentStatus paymentStatus
) {
    public static OrderCancelResponse of(Order order, Payment payment) {
        // 최소 처리된 주문과 결제 상태를 응답
        return new OrderCancelResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                payment.getStatus()
        );
    }
}
