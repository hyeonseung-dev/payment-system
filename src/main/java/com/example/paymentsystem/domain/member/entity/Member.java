package com.example.paymentsystem.domain.member.entity;

import com.example.paymentsystem.global.common.BaseEntity;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
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

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    private int pointBalance = 0;

    public Member(String email, String password, String name, String phoneNumber, int pointBalance) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.pointBalance = pointBalance;
    }

    public Member(String email, String password, String name, String phoneNumber) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public static Member create(String mail, String password, String name, String phoneNumber) {
        return new Member(mail, password, name, phoneNumber);
    }

    /**
     * 포인트 사용 (결제 시)
     *
     * @param amount 사용할 포인트
     * @throws BusinessException 잔액 부족 시 {@link ErrorCode#INSUFFICIENT_POINT}
     */
    public void usePoint(int amount) {

        if (amount <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_POINT_AMOUNT
            );
        }

        if (this.pointBalance < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }
        this.pointBalance -= amount;
    }

    /**
     * 포인트 적립 (결제 완료 시)
     *
     * @param amount 적립할 포인트
     */
    public void earnPoint(int amount) {
        if (amount <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_POINT_AMOUNT
            );
        }

        this.pointBalance += amount;
    }

    /**
     * 포인트 복구 (환불 시 사용분 반환)
     *
     * @param amount 복구할 포인트
     */
    public void restoreUsePoint(int amount) {

        if (amount <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_POINT_AMOUNT
            );
        }

        this.pointBalance += amount;
    }

    /**
     * 포인트 회수 (환불 시 적립분 차감)
     * 즉시 적립 정책으로 인해 음수 잔액 허용
     *
     * @param amount 회수할 포인트
     */
    public void cancelEarnPoint(int amount) {
        if (amount <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_POINT_AMOUNT
            );
        }

        this.pointBalance -= amount;
    }
}

