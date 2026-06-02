package com.example.paymentsystem.domain.product.dto.response;

public record ProductDetailResponse(
        Long productId,
        String name,
        Integer price,
        Integer stockQuantity,
        String description,
        String category,
        String status
) {
}
