package com.example.paymentsystem.domain.cart.controller;

import com.example.paymentsystem.domain.cart.dto.*;
import com.example.paymentsystem.domain.cart.service.CartService;
import com.example.paymentsystem.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // todo JWT 구현 완료 후 memberId는 인증 객체에서 꺼내도록 수정 예정
    private static final Long MEMBER_ID = 1L;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> findCartItems() {
        CartResponse response = cartService.findCartItems(MEMBER_ID);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<AddCartResponse>> addItem(@Valid @RequestBody AddCartRequest request) {
        Long addItem = cartService.addItem(MEMBER_ID, request.productId(), request.quantity());

        AddCartResponse response = AddCartResponse.of(
                addItem,
                request.productId(),
                request.quantity()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<UpdateCartResponse>> updateQuantity(@PathVariable Long cartItemId, @Valid @RequestBody UpdateCartRequest request) {
        UpdateCartResponse response = cartService.updateQuantity(MEMBER_ID, cartItemId, request.quantity());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(@PathVariable Long cartItemId) {
        cartService.removeItem(MEMBER_ID, cartItemId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
