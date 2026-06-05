package com.example.paymentsystem.domain.order.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderPreviewRequest(
        @NotEmpty(message = "주문할 장바구니 상품을 선택해주세요")
        List<Long> cartItemIds
) {
}
