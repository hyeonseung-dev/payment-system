package com.example.paymentsystem.domain.order.entity;

import com.example.paymentsystem.domain.cart.entity.CartItem;
import com.example.paymentsystem.domain.product.entity.Product;
import com.example.paymentsystem.global.common.BaseEntity;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 상품의 상품 스냅샷과 주문 수량을 관리하는 엔티티이다.
 */
@Getter
@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long productId;

    private Long cartItemId;

    @Column(nullable = false, length = 100)
    private String productNameSnapshot;

    @Column(nullable = false)
    private int productPriceSnapshot;

    @Column(nullable = false)
    private int quantity;

    private OrderItem(Order order, Product product, int quantity, Long cartItemId) {
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }

        this.order = order;
        this.productId = product.getId();
        this.cartItemId = cartItemId;
        this.productNameSnapshot = product.getName();
        this.productPriceSnapshot = product.getPrice();
        this.quantity = quantity;
    }

    public static OrderItem createSnapshot(Order order, CartItem cartItem) {
        // 장바구니 상품 기준으로 주문 상품 스냅샷을 생성한다.
        return new OrderItem(order, cartItem.getProduct(), cartItem.getQuantity(), cartItem.getId());
    }

    public static OrderItem createProductSnapshot(Order order, Product product, int quantity) {
        // 상품 바로 주문에서는 CartItem이 없으므로 Product와 quantity로 스냅샷을 생성한다.
        return new OrderItem(order, product, quantity, null);
    }
}
