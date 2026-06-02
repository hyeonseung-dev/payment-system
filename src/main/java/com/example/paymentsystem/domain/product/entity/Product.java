package com.example.paymentsystem.domain.product.entity;

import com.example.paymentsystem.global.common.BaseEntity;
import com.example.paymentsystem.domain.product.enumtype.ProductCategory;
import com.example.paymentsystem.domain.product.enumtype.ProductStatus;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer stockQuantity;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Builder
    private Product(
            String name,
            Integer price,
            Integer stockQuantity,
            String description,
            ProductCategory category,
            ProductStatus status
    ) {
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.description = description;
        this.category = category;
        this.status = status;
    }

}
