package com.example.paymentsystem.domain.point.controller;

import com.example.paymentsystem.domain.point.dto.response.PointHistoryPageResponse;
import com.example.paymentsystem.domain.point.dto.response.PointBalanceResponse;
import com.example.paymentsystem.domain.point.service.PointService;
import com.example.paymentsystem.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<PointBalanceResponse>> getBalance(
            @AuthenticationPrincipal Long memberId
    ) {
        PointBalanceResponse response =
                pointService.getBalance(memberId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/histories")
    public ResponseEntity<ApiResponse<PointHistoryPageResponse>> getPointHistories(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        PointHistoryPageResponse response =
                pointService.getPointHistories(memberId, page, size);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
