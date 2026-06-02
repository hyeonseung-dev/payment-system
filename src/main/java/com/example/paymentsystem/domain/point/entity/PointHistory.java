package com.example.paymentsystem.domain.point.entity;

import com.example.paymentsystem.global.common.BaseEntity;
import jakarta.persistence.*;
import com.example.paymentsystem.domain.point.enumtype.PointHistoryType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "point_histories")
public class PointHistory extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long pointHistoryId;

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointHistoryType type;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private Integer balanceAfter;
}
