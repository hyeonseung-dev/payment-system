package com.example.paymentsystem.domain.point.controller;

import com.example.paymentsystem.domain.point.dto.response.PointBalanceResponse;
import com.example.paymentsystem.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;
}
