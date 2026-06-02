package com.example.paymentsystem.domain.product.service;

import com.example.paymentsystem.domain.product.dto.response.ProductDetailResponse;
import com.example.paymentsystem.domain.product.dto.response.ProductPageResponse;
import com.example.paymentsystem.domain.product.dto.response.ProductResponse;
import com.example.paymentsystem.domain.product.repository.ProductRepository;
import com.example.paymentsystem.domain.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public ProductPageResponse getProducts() {
        List<ProductResponse> products =
                productRepository.findAll()
                        .stream()
                        .map(product -> new ProductResponse(
                                product.getId(),
                                product.getName(),
                                product.getPrice(),
                                product.getStockQuantity(),
                                product.getCategory(),
                                product.getStatus()
                        ))
                        .toList();
        return new ProductPageResponse(
                products,
                0,
                products.size(),
                products.size(),
                1
        );
    }

    public ProductDetailResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getDescription(),
                product.getCategory(),
                product.getStatus()
        );
    }
}
