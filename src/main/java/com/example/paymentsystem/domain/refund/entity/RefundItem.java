package com.example.paymentsystem.domain.refund.entity;

import com.example.paymentsystem.domain.order.entity.OrderItem;
import com.example.paymentsystem.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 환불 요청에 포함된 주문 상품별 환불 수량과 금액을 관리하는 엔티티이다.
 */
@Getter
@Entity
@Table(name = "refund_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id", nullable = false)
    private Refund refund;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Long pointRefundAmount;

    @Column(nullable = false)
    private Long pgRefundAmount;
}
