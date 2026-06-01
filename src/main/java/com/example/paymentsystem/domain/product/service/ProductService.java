package com.example.paymentsystem.domain.product.service;

import com.example.paymentsystem.domain.product.dto.response.ProductPageResponse;
import com.example.paymentsystem.domain.product.dto.response.ProductResponse;
import com.example.paymentsystem.domain.product.repository.ProductRepository;
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
                                product.getProductId(),
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
}
