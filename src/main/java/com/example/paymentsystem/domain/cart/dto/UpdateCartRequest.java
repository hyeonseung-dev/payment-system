package com.example.paymentsystem.domain.cart.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartRequest(
        @Min(value = 1, message = "수량은 1이상이어야 합니다.")
        int quantity) {
}