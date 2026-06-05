package com.example.paymentsystem.domain.order.service;

import com.example.paymentsystem.domain.cart.entity.CartItem;
import com.example.paymentsystem.domain.product.entity.Product;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderValidator {

    public void validateCartItems(List<CartItem> cartItems, List<Long> requestedCartItemIds) {
        // 요청한 장바구니 상품 중 하나라도 없거나 다른 회원의 상품이면 예외 처리한다.
        if (cartItems.size() != requestedCartItemIds.size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
    }

    public void validateStock(List<CartItem> cartItems) {

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
        }
    }
}
