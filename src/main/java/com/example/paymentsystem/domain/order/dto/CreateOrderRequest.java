package com.example.paymentsystem.domain.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "주문할 장바구니 상품을 선택해주세요.")
        List<Long> cartItemIds,

        // int말고 Integer사용한 이유가 필드를 아예 안 보내도 되게 하기 위해서
        @PositiveOrZero(message = "사용 포인트는 0 이상이어야 합나디.")
        Integer usePointAmount
) {
    public int getUsePointAmount() {
        // 포인트 필드를 안 보내면 0으로 처리한다.
        return usePointAmount == null ? 0 : usePointAmount;
    }

}
