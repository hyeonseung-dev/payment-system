package com.example.paymentsystem.domain.cart.controller;

import com.example.paymentsystem.domain.cart.dto.AddCartRequest;
import com.example.paymentsystem.domain.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // todo JWT 구현 완료 후 memberId는 인증 객체에서 꺼내도록 수정 예정
    private static final Long MEMBER_ID = 1L;


    @PostMapping("/items")
    public ResponseEntity<Long> addItem(@Valid @RequestBody AddCartRequest request) {
        Long addItem = cartService.addItem(
                MEMBER_ID,
                request.productId(),
                request.quantity()
        );
        return ResponseEntity.ok(addItem);
    }
}
