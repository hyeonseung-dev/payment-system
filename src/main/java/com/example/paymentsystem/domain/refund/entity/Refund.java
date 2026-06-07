package com.example.paymentsystem.domain.refund.entity;

import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 환불 요청 1건의 금액과 처리 상태를 관리하는 엔티티이다.
 */
@Getter
@Entity
@Table(name = "refunds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false)
    private Long totalRefundAmount;

    @Column(nullable = false)
    private Long pointRefundAmount;

    @Column(nullable = false)
    private Long pgRefundAmount;

    @Column(nullable = false)
    private Long earnedPointCancelAmount;

    @Column(nullable = false)
    private Long earnedPointDeductionAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundStatus status;

    private Refund(
            Payment payment,
            String reason,
            Long totalRefundAmount,
            Long pointRefundAmount,
            Long pgRefundAmount,
            Long earnedPointCancelAmount,
            Long earnedPointDeductionAmount,
            RefundStatus status
    ) {
        this.payment = payment;
        this.reason = reason;
        this.totalRefundAmount = totalRefundAmount;
        this.pointRefundAmount = pointRefundAmount;
        this.pgRefundAmount = pgRefundAmount;
        this.earnedPointCancelAmount = earnedPointCancelAmount;
        this.earnedPointDeductionAmount = earnedPointDeductionAmount;
        this.status = status;
    }

    /**
     * 정상 완료된 환불 정보를 생성한다.
     *
     * @param payment 환불 대상 결제
     * @param reason 환불 사유
     * @param totalRefundAmount 전체 환불 금액
     * @param pointRefundAmount 포인트 환불 금액
     * @param pgRefundAmount PG 환불 금액
     * @param earnedPointCancelAmount 적립 포인트 회수 금액
     * @param earnedPointDeductionAmount 적립 포인트 부족으로 PG 환불액에서 차감한 금액
     * @return 완료 상태의 환불 엔티티
     */
    public static Refund createCompleted(
            Payment payment,
            String reason,
            Long totalRefundAmount,
            Long pointRefundAmount,
            Long pgRefundAmount,
            Long earnedPointCancelAmount,
            Long earnedPointDeductionAmount
    ) {
        return new Refund(
                payment,
                reason,
                totalRefundAmount,
                pointRefundAmount,
                pgRefundAmount,
                earnedPointCancelAmount,
                earnedPointDeductionAmount,
                RefundStatus.COMPLETED
        );
    }

    /**
     * 실패한 환불 정보를 생성한다.
     *
     * @param payment 환불 대상 결제
     * @param reason 환불 사유
     * @param totalRefundAmount 전체 환불 금액
     * @param pointRefundAmount 포인트 환불 금액
     * @param pgRefundAmount PG 환불 금액
     * @param earnedPointCancelAmount 적립 포인트 회수 금액
     * @param earnedPointDeductionAmount 적립 포인트 부족으로 PG 환불액에서 차감한 금액
     * @return 실패 상태의 환불 엔티티
     */
    public static Refund createFailed(
            Payment payment,
            String reason,
            Long totalRefundAmount,
            Long pointRefundAmount,
            Long pgRefundAmount,
            Long earnedPointCancelAmount,
            Long earnedPointDeductionAmount
    ) {
        return new Refund(
                payment,
                reason,
                totalRefundAmount,
                pointRefundAmount,
                pgRefundAmount,
                earnedPointCancelAmount,
                earnedPointDeductionAmount,
                RefundStatus.FAILED
        );
    }
}
