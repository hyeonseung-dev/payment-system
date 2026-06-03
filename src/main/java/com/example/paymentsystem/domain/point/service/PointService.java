package com.example.paymentsystem.domain.point.service;

import com.example.paymentsystem.domain.point.repository.PointHistoryRepository;
import com.example.paymentsystem.domain.point.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;
    private final PointHistoryRepository pointHistoryRepository;
}
