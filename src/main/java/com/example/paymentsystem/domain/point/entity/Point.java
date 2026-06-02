package com.example.paymentsystem.domain.point.entity;

import com.example.paymentsystem.global.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "points")
public class Point extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pointId;

    private Long memberId;
    private Integer pointBalance;
}
