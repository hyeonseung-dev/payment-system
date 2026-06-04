package com.example.paymentsystem.domain.order.service;

import com.example.paymentsystem.domain.cart.entity.CartItem;
import com.example.paymentsystem.domain.cart.repository.CartItemRepository;
import com.example.paymentsystem.domain.order.dto.OrderPreviewRequest;
import com.example.paymentsystem.domain.order.dto.OrderPreviewResponse;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartItemRepository cartItemRepository;

    @Transactional(readOnly = true)
    public OrderPreviewResponse previewOrder(Long memberId, OrderPreviewRequest request) {

        List<CartItem> cartItems = cartItemRepository.findAllByIdsAndMemberId(request.cartItemIds(), memberId);

        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        if (cartItems.size() != request.cartItemIds().size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        return OrderPreviewResponse.from(cartItems);
    }
}