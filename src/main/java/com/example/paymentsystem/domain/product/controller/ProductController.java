package com.example.paymentsystem.domain.product.controller;

import com.example.paymentsystem.domain.product.dto.response.ProductDetailResponse;
import com.example.paymentsystem.domain.product.dto.response.ProductPageResponse;
import com.example.paymentsystem.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<ProductPageResponse> getProducts()
    {return ResponseEntity.ok(productService.getProducts());}

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProduct(@PathVariable Long productId)
    {return ResponseEntity.ok(productService.getProduct(productId));}

}
