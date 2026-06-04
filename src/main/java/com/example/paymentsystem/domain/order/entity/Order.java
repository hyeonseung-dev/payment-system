package com.example.paymentsystem.domain.order.entity;

import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.domain.order.enums.OrderStatus;
import com.example.paymentsystem.global.common.BaseEntity;
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

}
