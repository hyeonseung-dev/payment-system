package com.example.paymentsystem.domain.product.dto.response;

import com.example.paymentsystem.domain.product.enumtype.ProductCategory;
import com.example.paymentsystem.domain.product.enumtype.ProductStatus;

public record ProductResponse(
        Long productId,
        String name,
        Integer price,
        Integer stockQuantity,
        ProductCategory category,
        ProductStatus status,
        String imageUrl
) {
}
