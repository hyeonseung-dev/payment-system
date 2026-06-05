package com.example.paymentsystem.domain.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateProductOrderRequest(

        @NotNull(message = "상품을 선택해주세요.")
        Long productId,

        @Positive(message = "수량은 1이상이어야 합니다.")
        int quantity,

        @PositiveOrZero(message = "사용 포인트는 0이상이어야 합니다.")
        Integer pointAmount
) {
    public int getusePointAmount() {
        return pointAmount == null ? 0 : pointAmount;
    }
}
