package com.example.paymentsystem.domain.point.entity;

import com.example.paymentsystem.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import com.example.paymentsystem.domain.point.enumtype.PointHistoryType;

@Entity
@Table(name = "point_histories")
public class PointHistory extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pointHistoryId;
    private Long paymentId;
    private Long memberId;

    @Enumerated(EnumType.STRING)
    private PointHistoryType type;
    private Integer amount;
    private Integer balanceAfter;
}
