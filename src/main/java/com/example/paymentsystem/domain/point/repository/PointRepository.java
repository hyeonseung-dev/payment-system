package com.example.paymentsystem.domain.point.repository;

import com.example.paymentsystem.domain.point.entity.Point;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointRepository extends JpaRepository<Point, Long>{
}
