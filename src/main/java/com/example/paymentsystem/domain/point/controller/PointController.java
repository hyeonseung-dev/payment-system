package com.example.paymentsystem.domain.point.controller;

import com.example.paymentsystem.domain.point.dto.response.PointHistoryPageResponse;
import com.example.paymentsystem.domain.point.dto.response.PointBalanceResponse;
import com.example.paymentsystem.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;

    @GetMapping("/balance")
    public ResponseEntity<PointBalanceResponse> getBalance(
            Authentication authentication
    ) {
        Long memberId = Long.parseLong(authentication.getName());

        PointBalanceResponse response = pointService.getBalance(memberId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/histories")
    public ResponseEntity<PointHistoryPageResponse> getPointHistories(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long memberId = Long.parseLong(authentication.getName());

        PointHistoryPageResponse response = pointService.getPointHistories(memberId, page, size);
        return ResponseEntity.ok(response);
    }
}