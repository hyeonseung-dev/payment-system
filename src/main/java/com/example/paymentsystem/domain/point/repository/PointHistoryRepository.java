package com.example.paymentsystem.domain.point.repository;

import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.domain.point.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PointHistoryRepository
        extends JpaRepository<PointHistory, Long> {

    Page<PointHistory> findByMemberOrderByCreatedAtDesc(
            Member member,
            Pageable pageable
    );
}