package com.example.paymentsystem.domain.product.dto.response;

public record ProductResponse(
        Long productId,
        String name,
        Integer price,
        Integer stockQuantity,
        String category,
        String status
) {
}
