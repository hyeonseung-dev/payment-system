package com.example.paymentsystem.domain.order.repository;

import com.example.paymentsystem.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 로그인한 회원의 주문 목록을 최신순으로 조회
    Page<Order> findByMember_IdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    Optional<Order> findByIdAndMember_Id(Long orderId, Long memberId);
}
