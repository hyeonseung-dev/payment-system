package com.example.paymentsystem.domain.order.dto;

import java.util.List;

public record OrderListResponse(
        List<OrderListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static OrderListResponse of(
            List<OrderListItemResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        return new OrderListResponse(
                content,
                page,
                size,
                totalElements,
                totalPages
        );
    }
}
