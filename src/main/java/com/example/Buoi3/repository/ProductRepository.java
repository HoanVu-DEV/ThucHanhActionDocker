package com.example.Buoi3.repository;

import com.example.Buoi3.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCase(String keyword);
    List<Product> findByDiscountTrue();
    List<Product> findByCategoryId(Long categoryId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.category.id = :categoryId OR p.category.parent.id = :categoryId")
    List<Product> findByCategoryOrParentId(@org.springframework.data.repository.query.Param("categoryId") Long categoryId);
}
