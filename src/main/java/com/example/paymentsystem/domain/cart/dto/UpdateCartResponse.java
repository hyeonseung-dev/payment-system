package com.example.paymentsystem.domain.cart.dto;

public record UpdateCartResponse(
        Long cartItemId,
        int quantity) {
}