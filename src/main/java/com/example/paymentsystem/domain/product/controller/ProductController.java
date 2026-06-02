package com.example.paymentsystem.domain.product.controller;

import com.example.paymentsystem.domain.product.dto.response.ProductDetailResponse;
import com.example.paymentsystem.domain.product.dto.response.ProductPageResponse;
import com.example.paymentsystem.domain.product.service.ProductService;
import com.example.paymentsystem.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProductPageResponse>> getProducts() {
        ProductPageResponse response = productService.getProducts();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.ok(response));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProduct(@PathVariable Long productId) {
        ProductDetailResponse response = productService.getProduct(productId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.ok(response));
    }
}
