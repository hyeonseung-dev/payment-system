package com.example.paymentsystem.domain.order.repository;

import com.example.paymentsystem.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id IN :orderIds ORDER BY oi.id ASC")
    List<OrderItem> findAllByOrderIds(@Param("orderIds")List<Long> orderIds);
    List<OrderItem> findAllByOrder_Id(Long orderId);

    /**
     * 주문 ID로 주문 상품에 저장된 원본 장바구니 상품 ID 목록을 조회한다.
     *
     * @param orderId 주문 ID
     * @return 원본 장바구니 상품 ID 목록
     */
    @Query("""
            select oi.cartItemId
            from OrderItem oi
            where oi.order.id = :orderId
              and oi.cartItemId is not null
            """)
    List<Long> findCartItemIdsByOrderId(@Param("orderId") Long orderId);
}
