package com.example.paymentsystem.domain.order.repository;

import com.example.paymentsystem.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
