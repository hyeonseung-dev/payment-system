package com.example.paymentsystem.domain.order.dto;

import com.example.paymentsystem.domain.order.entity.Order;
import com.example.paymentsystem.domain.order.entity.OrderItem;
import com.example.paymentsystem.domain.order.enums.OrderStatus;
import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.domain.payment.entity.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse (
        Long orderId,
        String orderNumber,
        OrderStatus status,
        int totalAmount,
        int pointAmount,
        Long pgAmount,
        LocalDateTime createAt,
        PaymentStatus paymentStatus,
        Long earnedPointAmount,
        List<OrderDetailItemResponse> items
) {
    public static OrderDetailResponse of(Order order, List<OrderItem> orderItems, Payment payment) {
        // 주문 생성 시점에 저장한 포인트 사용 금액
        int pointAmount = order.getUsePointAmountSnapshot();

        // Payment에 저장된 실제 PG 결제 금액을 사용한다
        Long pgAmount = payment.getPgAmount();

        // 주문 상품 엔티티를 상세 응답 DTO로 변환
        List<OrderDetailItemResponse> items = orderItems.stream()
                .map(OrderDetailItemResponse::from)
                .toList();

        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                pointAmount,
                pgAmount,
                order.getCreatedAt(),
                payment.getStatus(),
                payment.getEarnedPointAmount(),
                items
        );
    }
}
