package com.example.paymentsystem.domain.cart.controller;

import com.example.paymentsystem.domain.cart.dto.*;
import com.example.paymentsystem.domain.cart.service.CartService;
import com.example.paymentsystem.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> findCartItems(@AuthenticationPrincipal Long memberId) {
        CartResponse response = cartService.findCartItems(memberId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<AddCartResponse>> addItem(@AuthenticationPrincipal Long memberId, @Valid @RequestBody AddCartRequest request) {
        Long addItem = cartService.addItem(memberId, request.productId(), request.quantity());

        AddCartResponse response = AddCartResponse.of(
                addItem,
                request.productId(),
                request.quantity()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<UpdateCartResponse>> updateQuantity(@AuthenticationPrincipal Long memberId, @PathVariable Long cartItemId, @Valid @RequestBody UpdateCartRequest request) {
        UpdateCartResponse response = cartService.updateQuantity(memberId, cartItemId, request.quantity());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(@AuthenticationPrincipal Long memberId, @PathVariable Long cartItemId) {
        cartService.removeItem(memberId, cartItemId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
