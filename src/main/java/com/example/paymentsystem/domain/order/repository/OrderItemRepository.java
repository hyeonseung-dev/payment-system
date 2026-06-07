package com.example.paymentsystem.domain.order.repository;

import com.example.paymentsystem.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id IN :orderIds ORDER BY oi.id ASC")
    List<OrderItem> findAllByOrderIds(@Param("orderIds")List<Long> orderIds);

    // 특정 주문의 주문 상품 목록을 조회한다.
    List<OrderItem> findAllByOrder_IdOrderByIdAsc(Long orderId);
}
