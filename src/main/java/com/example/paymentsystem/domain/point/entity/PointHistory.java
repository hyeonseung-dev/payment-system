package com.example.paymentsystem.domain.point.entity;

import com.example.paymentsystem.domain.point.enumtype.PointHistoryType;
import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.global.common.BaseEntity;
import lombok.NoArgsConstructor;
import lombok.Getter;
import jakarta.persistence.*;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "point_histories")
public class PointHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_history_id")
    private Long id;

    @Column(nullable = false)
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointHistoryType type;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private Integer balanceAfter;

    private PointHistory(
            Long paymentId,
            Member member,
            PointHistoryType type,
            Integer amount,
            Integer balanceAfter
    ) {
        this.paymentId = paymentId;
        this.member = member;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public static PointHistory createUseHistory(
            Long paymentId,
            Member member,
            Integer amount,
            Integer balanceAfter
    ) {
        return new PointHistory(
                paymentId,
                member,
                PointHistoryType.USE,
                -amount,
                balanceAfter
        );
    }

    public static PointHistory createEarnHistory(
            Long paymentId,
            Member member,
            Integer amount,
            Integer balanceAfter
    ) {
        return new PointHistory(
                paymentId,
                member,
                PointHistoryType.EARN,
                amount,
                balanceAfter
        );
    }

    public static PointHistory createUseCancelHistory(
            Long paymentId,
            Member member,
            Integer amount,
            Integer balanceAfter
    ) {
        return new PointHistory(
                paymentId,
                member,
                PointHistoryType.USE_CANCEL,
                amount,
                balanceAfter
        );
    }

    public static PointHistory createEarnCancelHistory(
            Long paymentId,
            Member member,
            Integer amount,
            Integer balanceAfter
    ) {
        return new PointHistory(
                paymentId,
                member,
                PointHistoryType.EARN_CANCEL,
                -amount,
                balanceAfter
        );
    }
}