package com.example.Buoi3.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên sản phẩm là bắt buộc")
    private String name;

    @NotNull(message = "Giá là bắt buộc")
    @Positive(message = "Giá phải lớn hơn 0")
    private double price;

    private String description;

    @Column(nullable = false)
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean discount = false;

    // Phase 5 Promotional limits
    @Column(name = "promo_price")
    private Double promoPrice;

    @Column(name = "promo_quantity")
    private Integer promoQuantity;
}