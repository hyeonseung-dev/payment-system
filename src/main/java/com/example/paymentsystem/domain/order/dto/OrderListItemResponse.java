package com.example.paymentsystem.domain.order.dto;

import com.example.paymentsystem.domain.order.entity.Order;
import com.example.paymentsystem.domain.order.entity.OrderItem;
import com.example.paymentsystem.domain.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderListItemResponse(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        int itemCount,
        int totalAmount,
        LocalDateTime createAt
) {
    public static OrderListItemResponse from(Order order, List<OrderItem> orderItems) {

        return new OrderListItemResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                orderItems.size(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }
}
