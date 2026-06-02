package com.example.paymentsystem.domain.cart.entity;

import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;


    public static Cart create(Member member) {
        Cart cart = new Cart();
        cart.member = member;
        return cart;
    }
}
