package com.example.paymentsystem.domain.point.controller;

import com.example.paymentsystem.domain.point.dto.response.PointHistoryPageResponse;
import com.example.paymentsystem.domain.point.service.PointService;
import com.example.paymentsystem.domain.point.dto.response.PointBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;

    @GetMapping("/balance")
    public PointBalanceResponse getBalance() {
        return pointService.getBalance();
    }

    @GetMapping("/histories")
    public PointHistoryPageResponse getPointHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return pointService.getPointHistories(page, size);
    }

}