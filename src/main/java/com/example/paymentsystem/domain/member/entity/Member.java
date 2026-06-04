package com.example.paymentsystem.domain.member.entity;

import com.example.paymentsystem.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    private int pointBalance = 0;

    public void usePoint(int amout) {
        if (pointBalance < amout) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }

        pointBalance -= amout;
    }

    public void earnPoint(int amount) {
        pointBalance += amount;
    }

    public void cancelEarnPoint(int amount) {
        if(pointBalance < amount) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }

        pointBalance -= amount;
    }

    public Member(String email, String password, String name, String phoneNumber, int pointBalance) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.pointBalance = pointBalance;
    }
}
