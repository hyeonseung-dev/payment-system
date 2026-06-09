package com.example.paymentsystem.domain.product.service;

import java.util.function.Supplier;
import com.example.paymentsystem.domain.product.dto.response.ProductDetailResponse;
import com.example.paymentsystem.domain.product.dto.response.ProductPageResponse;
import com.example.paymentsystem.domain.product.dto.response.ProductResponse;
import com.example.paymentsystem.domain.product.repository.ProductRepository;
import com.example.paymentsystem.domain.product.entity.Product;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

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
                                product.getStatus(),
                                product.getImageUrl()
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
                product.getStatus(),
                product.getImageUrl()
        );
    }
}
