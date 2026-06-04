package com.example.paymentsystem.domain.cart.dto;

import java.util.List;

public record CartResponse(
        Long cartId,
        List<CartItemResponse> items,
        int totalAmount
) {
    public static CartResponse of(Long cartId, List<CartItemResponse> items) {
        int totalAmount = items.stream()
                .mapToInt(CartItemResponse::subtotal)
                .sum();

        return new CartResponse(cartId, items, totalAmount);
    }
}
