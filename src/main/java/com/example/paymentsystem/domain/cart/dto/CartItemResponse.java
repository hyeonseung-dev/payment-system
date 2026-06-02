package com.example.paymentsystem.domain.cart.dto;

import com.example.paymentsystem.domain.cart.entity.CartItem;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        int price,
        int quantity,
        int subtotal
) {

    public static CartItemResponse from(CartItem item) {
        int subtotal = item.getProduct().getPrice() * item.getQuantity();

        return new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getPrice(),
                item.getQuantity(),
                subtotal
        );
    }
}
