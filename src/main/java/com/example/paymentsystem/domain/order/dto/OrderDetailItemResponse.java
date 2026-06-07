package com.example.paymentsystem.domain.order.dto;

import com.example.paymentsystem.domain.order.entity.OrderItem;

public record OrderDetailItemResponse(
        Long orderItemId,
        Long productId,
        String productName,
        int price,
        int quantity,
        int subtotal

) {
    public static OrderDetailItemResponse from(OrderItem orderItem) {
        // 주문 당시 저장해둔 상품 스냅샷 기준으로 응답을 만든다.
        int subtotal = orderItem.getProductPriceSnapshot() * orderItem.getQuantity();

        return new OrderDetailItemResponse(
                orderItem.getId(),
                orderItem.getProductId(),
                orderItem.getProductNameSnapshot(),
                orderItem.getProductPriceSnapshot(),
                orderItem.getQuantity(),
                subtotal
        );
    }
}