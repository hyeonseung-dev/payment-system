package com.example.paymentsystem.domain.product.controller;

import com.example.paymentsystem.domain.product.dto.response.ProductDetailResponse;
import com.example.paymentsystem.domain.product.dto.response.ProductPageResponse;
import com.example.paymentsystem.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
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
    public ProductPageResponse getProducts() {
        return productService.getProducts();
    }

    @GetMapping("/{productId}")
    public ProductDetailResponse getProduct(
            @PathVariable Long productId
    ) {
        return productService.getProduct(productId);
    }
}
