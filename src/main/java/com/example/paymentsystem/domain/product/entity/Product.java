package com.example.paymentsystem.domain.product.entity;

import com.example.paymentsystem.global.common.BaseEntity;
import com.example.paymentsystem.domain.product.enumtype.ProductCategory;
import com.example.paymentsystem.domain.product.enumtype.ProductStatus;
import jakarta.persistence.*;
import tools.jackson.core.ObjectReadContext;


@Entity
@Table(name = "products")
public class Product extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    private String name;
    private Integer price;
    private Integer stockQuantity;
    private String description;

    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

}
