package com.example.paymentsystem.domain.product.repository;

import com.example.paymentsystem.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
