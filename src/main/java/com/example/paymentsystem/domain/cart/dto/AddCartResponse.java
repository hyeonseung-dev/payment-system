package com.example.paymentsystem.domain.cart.dto;

public record AddCartResponse(
        Long cartItemId,
        Long productId,
        int quantity) {

    public static AddCartResponse of(Long cartItemId, Long productId, int quantity) {
        return new AddCartResponse(cartItemId, productId, quantity);
    }
}
