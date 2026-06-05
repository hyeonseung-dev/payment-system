package com.example.paymentsystem.domain.order.dto;

import com.example.paymentsystem.domain.cart.entity.CartItem;
import com.example.paymentsystem.domain.product.entity.Product;

public record OrderPreviewItemResponse(
        Long cartItemId,
        Long productId,
        String productName,
        int price,
        int quantity,
        int subtotal
) {

    public static OrderPreviewItemResponse from(CartItem cartItem) {
        Product product = cartItem.getProduct();
        int subtotal = product.getPrice() * cartItem.getQuantity();

        return new OrderPreviewItemResponse(
                cartItem.getId(),
                product.getId(),
                product.getName(),
                product.getPrice(),
                cartItem.getQuantity(),
                subtotal
        );
    }
}
