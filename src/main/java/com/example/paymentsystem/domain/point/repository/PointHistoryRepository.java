package com.example.paymentsystem.domain.point.repository;

import com.example.paymentsystem.domain.point.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
}
