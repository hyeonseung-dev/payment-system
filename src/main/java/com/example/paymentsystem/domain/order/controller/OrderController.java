package com.example.paymentsystem.domain.order.controller;

import com.example.paymentsystem.domain.order.dto.*;
import com.example.paymentsystem.domain.order.service.OrderService;
import com.example.paymentsystem.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<OrderPreviewResponse>> previewOrder(
            @AuthenticationPrincipal Long memberId, @Valid @RequestBody OrderPreviewRequest request) {
        OrderPreviewResponse response = orderService.previewOrder(memberId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CreateOrderRequest request) {
        CreateOrderResponse response = orderService.createOrder(memberId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createProductOrder(
            @AuthenticationPrincipal Long memberId, @Valid @RequestBody CreateProductOrderRequest request) {
        CreateOrderResponse response = orderService.createProductOrder(memberId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<OrderListResponse>> findOrders(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        OrderListResponse response = orderService.findOrder(memberId, page, size);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}