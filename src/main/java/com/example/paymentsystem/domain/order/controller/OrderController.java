package com.example.paymentsystem.domain.order.controller;

import com.example.paymentsystem.domain.order.dto.OrderPreviewRequest;
import com.example.paymentsystem.domain.order.dto.OrderPreviewResponse;
import com.example.paymentsystem.domain.order.service.OrderService;
import com.example.paymentsystem.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // todo JWT 구현 완료 후 memberId는 인증 객체에서 꺼내도록 수정 예정
    private static final Long MEMBER_ID = 1L;

    @PostMapping("/orders/preview")
    public ResponseEntity<ApiResponse<OrderPreviewResponse>> previewOrder(
            @Valid @RequestBody OrderPreviewRequest request) {
        OrderPreviewResponse response = orderService.previewOrder(MEMBER_ID, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }
}