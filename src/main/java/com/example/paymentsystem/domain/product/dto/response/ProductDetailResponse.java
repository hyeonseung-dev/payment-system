package com.example.paymentsystem.domain.product.dto.response;

import com.example.paymentsystem.domain.product.enumtype.ProductCategory;
import com.example.paymentsystem.domain.product.enumtype.ProductStatus;

public record ProductDetailResponse(
        Long productId,
        String name,
        Integer price,
        Integer stockQuantity,
        String description,
        ProductCategory category,
        ProductStatus status,
        String imageUrl
) {
}
