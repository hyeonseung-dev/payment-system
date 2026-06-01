package com.example.paymentsystem.domain.cart.entity;

import com.example.paymentsystem.global.common.BaseEntity;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.example.paymentsystem.global.error.ErrorCode.INVALID_QUANTITY;

@Entity
@Table(name = "cart_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"cart_id", "product_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(nullable = false, columnDefinition = "int UNSIGNED DEFAULT 1")
    private int quantity;

    public CartItem(Product product, Cart cart, int quantity) {
        this.cart = cart;
        this.product = product;
        if(quantity < 1) {
            throw new IllegalArgumentException("수량은 1이상여야 합니다.");
        }
        this.quantity = quantity;
    }

    public static CartItem create(Cart cart, Product product, int quantity) {
        if(quantity <= 0) {
            throw new BusinessException(INVALID_QUANTITY);
        }

        CartItem cartItem = new CartItem();
        cartItem.cart = cart;
        cartItem.product = product;
        cartItem.quantity = quantity;
        return cartItem;
    }

    public Long getCartId() {
        return cart.getId();
    }

    public Long getProductId() {
        return product.getId();
    }

    public void addQuantity(int quantity) {
        if(quantity < 1) {
            throw new BusinessException(INVALID_QUANTITY);
        }
        this.quantity += quantity;
    }

    public void changeQuantity(int quantity) {
        if(quantity < 1) {
            throw new BusinessException(INVALID_QUANTITY);
        }
        this.quantity = quantity;
    }
}
