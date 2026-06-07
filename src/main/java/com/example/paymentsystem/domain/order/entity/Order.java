package com.example.paymentsystem.domain.order.entity;

import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.domain.order.enums.OrderStatus;
import com.example.paymentsystem.global.common.BaseEntity;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(nullable = false)
    private int totalAmount;

    @Column(nullable = false)
    private int usePointAmountSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private Order(Member member, String orderNumber, int totalAmount, int usePointAmountSnapshot, OrderStatus status) {
        this.member = member;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.usePointAmountSnapshot = usePointAmountSnapshot;  // 포인트 검증/사용은 결제 파트 주문 생성 시점 사용한 포인트가 없어서 기본값 0
        this.status = status;
    }

    // 주문 생성 시 기본 상태는 결제 대기
    public static Order createPending(Member member, String orderNumber, int totalAmount, int usePointAmountSnapshot) {
        // 주문 생성 시 기본 상태는 결제대기다.
        return new Order(member, orderNumber, totalAmount, usePointAmountSnapshot, OrderStatus.PAYMENT_PENDING);
    }

    // 결제 성공 시
    public void completePayment(int usePointAmountSnapshot) {
        // 결제 성공 시 사용한 포인트 금액을 주문 스냅샷에 저장한다.
        this.usePointAmountSnapshot = usePointAmountSnapshot;
        this.status = OrderStatus.COMPLETED;
    }

    // 주문 취소 시
    public void cancel() {
        // 결제대기 상태의 주문만 취소할 수 있다.
        if (this.status != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        this.status = OrderStatus.CANCELED;
    }
}
