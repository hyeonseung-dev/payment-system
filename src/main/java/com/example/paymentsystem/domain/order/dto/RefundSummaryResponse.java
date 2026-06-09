package com.example.paymentsystem.domain.order.dto;

import com.example.paymentsystem.domain.refund.entity.Refund;
import com.example.paymentsystem.domain.refund.entity.RefundStatus;

import java.time.LocalDateTime;

public record RefundSummaryResponse(
        Long refundId,
        RefundStatus status,
        String reason,
        Long totalRefundAmount,
        Long pgRefundAmount,
        Long pointRefundAmount,
        LocalDateTime createdAt
) {
    public static RefundSummaryResponse from(Refund refund) {
        return new RefundSummaryResponse(
                refund.getId(),
                refund.getStatus(),
                refund.getReason(),
                refund.getTotalRefundAmount(),
                refund.getPgRefundAmount(),
                refund.getPointRefundAmount(),
                refund.getCreatedAt()
        );
    }
}
