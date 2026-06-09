package com.example.paymentsystem.domain.order.dto;

import com.example.paymentsystem.domain.order.entity.Order;
import com.example.paymentsystem.domain.order.entity.OrderItem;
import com.example.paymentsystem.domain.order.enums.OrderStatus;
import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.domain.payment.entity.PaymentStatus;
import com.example.paymentsystem.domain.refund.entity.Refund;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        int totalAmount,
        int pointAmount,
        Long pgAmount,
        LocalDateTime createAt,
        PaymentStatus paymentStatus,
        Long earnedPointAmount,
        Long paymentId,
        List<OrderDetailItemResponse> items,
        List<RefundSummaryResponse> refunds
) {
    public static OrderDetailResponse of(Order order, List<OrderItem> orderItems, Payment payment, List<Refund> refunds) {
        int pointAmount = order.getUsePointAmountSnapshot();
        Long pgAmount = payment.getPgAmount();

        List<OrderDetailItemResponse> items = orderItems.stream()
                .map(OrderDetailItemResponse::from)
                .toList();

        List<RefundSummaryResponse> refundSummaries = refunds.stream()
                .map(RefundSummaryResponse::from)
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
                payment.getId(),
                items,
                refundSummaries
        );
    }
}
