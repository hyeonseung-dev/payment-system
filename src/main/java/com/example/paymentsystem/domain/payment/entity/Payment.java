package com.example.paymentsystem.domain.payment.entity;

import com.example.paymentsystem.domain.order.entity.Order;
import com.example.paymentsystem.global.common.BaseEntity;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 주문의 결제 정보를 관리하는 엔티티이다.
 */
@Getter
@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payments_order_id", columnNames = "order_id"),
                @UniqueConstraint(name = "uk_payments_portone_payment_id", columnNames = "portone_payment_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "portone_payment_id", nullable = false, length = 100)
    private String portonePaymentId;

    @Column(nullable = false)
    private Long totalAmount;

    @Column(nullable = false)
    private Long pgAmount;

    @Column(nullable = false)
    private Long earnedPointAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    private LocalDateTime paidAt;

    private Payment(
            Order order,
            String portonePaymentId,
            Long totalAmount,
            Long pgAmount,
            Long earnedPointAmount
    ) {
        this.order = order;
        this.portonePaymentId = portonePaymentId;
        this.totalAmount = totalAmount;
        this.pgAmount = pgAmount;
        this.earnedPointAmount = earnedPointAmount;
        this.status = PaymentStatus.READY;
    }

    /**
     * 결제 대기 상태의 결제 정보를 생성한다.
     *
     * @param order 주문
     * @param portonePaymentId PortOne 결제 식별자
     * @param totalAmount 주문 총 금액
     * @param pgAmount PG 결제 금액
     * @param earnedPointAmount 적립 예정 포인트
     * @return 결제 엔티티
     */
    public static Payment createReady(
            Order order,
            String portonePaymentId,
            Long totalAmount,
            Long pgAmount,
            Long earnedPointAmount
    ) {
        return new Payment(order, portonePaymentId, totalAmount, pgAmount, earnedPointAmount);
    }

    /**
     * 결제를 완료 상태로 변경한다.
     *
     * <p>READY 상태의 결제가 PortOne 결제 조회와 금액 검증을 통과했을 때 호출한다.
     * 결제 완료 시각도 함께 저장한다.</p>
     *
     * @param paidAt 결제 완료 시각
     */
    public void complete(LocalDateTime paidAt) {
        transitTo(PaymentStatus.PAID);
        this.paidAt = paidAt;
    }

    /**
     * 결제를 실패 상태로 변경한다.
     *
     * <p>PG 결제 실패, 결제 금액 불일치 등 결제가 정상 완료되지 못한 경우 호출한다.</p>
     */
    public void fail() {
        transitTo(PaymentStatus.FAILED);
    }

    /**
     * 결제를 부분 환불 상태로 변경한다.
     *
     * <p>결제 완료 후 주문 상품 중 일부만 환불되어 환불 가능 금액이 남아 있는 경우 호출한다.</p>
     */
    public void markPartialRefunded() {
        transitTo(PaymentStatus.PARTIAL_REFUNDED);
    }

    /**
     * 결제를 전체 환불 상태로 변경한다.
     *
     * <p>결제 완료 금액이 모두 환불되었거나, 부분 환불 이후 남은 금액까지 모두 환불된 경우 호출한다.</p>
     */
    public void markRefunded() {
        transitTo(PaymentStatus.REFUNDED);
    }

    /**
     * 결제가 대기 상태인지 확인한다.
     *
     * @return READY 상태이면 true
     */
    public boolean isReady() {
        return status == PaymentStatus.READY;
    }

    /**
     * 결제가 완료 상태인지 확인한다.
     *
     * @return PAID 상태이면 true
     */
    public boolean isPaid() {
        return status == PaymentStatus.PAID;
    }

    /**
     * 결제가 전체 환불 상태인지 확인한다.
     *
     * @return REFUNDED 상태이면 true
     */
    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED;
    }

    private void transitTo(PaymentStatus nextStatus) {
        if (!status.canTransitTo(nextStatus)) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        this.status = nextStatus;
    }
}
