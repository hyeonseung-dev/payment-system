package com.example.paymentsystem.domain.order.dto;


import com.example.paymentsystem.domain.cart.entity.CartItem;

import java.util.List;

public record OrderPreviewResponse(
        List<OrderPreviewItemResponse> items,
        int totalAmount
) {

    public static OrderPreviewResponse from(List<CartItem> cartItems) {
        List<OrderPreviewItemResponse> list = cartItems.stream()
                .map(OrderPreviewItemResponse::from)
                .toList();

        int totalAmount = list.stream()
                .mapToInt(OrderPreviewItemResponse::subtotal)
                .sum();

        return new OrderPreviewResponse(list, totalAmount);
    }

}