package com.example.Buoi3.Entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private Long productId;
    private String name;
    private double price;
    private String imageUrl;
    private int quantity;
    private Boolean isDiscount;
    private Double promoPrice;
    private Integer promoQuantity; // Max possible per DB
    private int reservedPromoQty;  // Actual secured reservation

    public double getItemTotalPrice() {
        if (isDiscount != null && isDiscount && promoPrice != null && reservedPromoQty > 0) {
            int promoItems = Math.min(quantity, reservedPromoQty);
            int regularItems = Math.max(0, quantity - reservedPromoQty);
            return (promoItems * promoPrice) + (regularItems * price);
        }
        return price * quantity;
    }
}
