package com.example.paymentsystem.domain.point.dto.response;

import com.example.paymentsystem.domain.point.enumtype.PointHistoryType;
import java.time.LocalDateTime;

public record PointHistoryResponse(
        Long pointHistoryId,
        Long paymentId,
        PointHistoryType type,
        Integer amount,
        Integer balanceAfter,
        LocalDateTime createdAt
) {
}
