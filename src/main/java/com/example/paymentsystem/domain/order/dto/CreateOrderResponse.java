package com.example.paymentsystem.domain.order.dto;

import com.example.paymentsystem.domain.order.entity.Order;
import com.example.paymentsystem.domain.order.enums.OrderStatus;
import com.example.paymentsystem.domain.payment.entity.Payment;

public record CreateOrderResponse(
        Long orderId,
        String orderNumber,
        String portonePaymentId,
        OrderStatus orderStatus,
        int totalAmount,
        int pointAmount,
        int pgAmount
) {

    // 결제 담당자가 사용할 주문 생성 응답을 만든다.
    public static CreateOrderResponse of(Order order, Payment payment, int pointAmount) {
        return new CreateOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                payment.getPortonePaymentId(),
                order.getStatus(),
                order.getTotalAmount(),
                pointAmount,
                payment.getPgAmount().intValue()
        );
    }
}
