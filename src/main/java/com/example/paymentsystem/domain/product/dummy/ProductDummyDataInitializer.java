package com.example.paymentsystem.domain.product.dummy;

import com.example.paymentsystem.domain.product.entity.Product;
import com.example.paymentsystem.domain.product.enumtype.ProductCategory;
import com.example.paymentsystem.domain.product.enumtype.ProductStatus;
import com.example.paymentsystem.domain.product.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductDummyDataInitializer {
    private final ProductRepository productRepository;

    @PostConstruct
    public void init() {

        if (productRepository.count() > 0) {
            return;
        }

        productRepository.save(
                Product.builder()
                        .name("아이폰 케이스")
                        .price(15000)
                        .stockQuantity(30)
                        .description("아이폰 전용 보호 케이스")
                        .category(ProductCategory.ELECTRONICS)
                        .status(ProductStatus.FOR_SALE)
                        .build()
        );

        productRepository.save(
                Product.builder()
                        .name("무선 마우스")
                        .price(35000)
                        .stockQuantity(20)
                        .description("블루투스 무선 마우스")
                        .category(ProductCategory.ELECTRONICS)
                        .status(ProductStatus.FOR_SALE)
                        .build()
        );

        productRepository.save(
                Product.builder()
                        .name("기계식 키보드")
                        .price(120000)
                        .stockQuantity(10)
                        .description("청축 기계식 키보드")
                        .category(ProductCategory.ELECTRONICS)
                        .status(ProductStatus.FOR_SALE)
                        .build()
        );
    }
}
