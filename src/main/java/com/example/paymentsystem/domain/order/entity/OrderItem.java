package com.example.paymentsystem.domain.order.entity;

import com.example.paymentsystem.global.common.BaseEntity;
import jakarta.persistence.*;

public class OrderItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 100)
    private String productNameSnapshot;

    @Column(nullable = false)
    private int productPriceSnapshot;

    @Column(nullable = false)
    private int quantity;
}
