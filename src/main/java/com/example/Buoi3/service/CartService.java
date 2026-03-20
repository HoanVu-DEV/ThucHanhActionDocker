package com.example.Buoi3.service;

import com.example.Buoi3.Entity.CartItem;
import com.example.Buoi3.Entity.Product;
import com.example.Buoi3.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@SessionScope
public class CartService {

    private List<CartItem> cartItems = new ArrayList<>();
    
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PromoReservationService reservationService;

    @Autowired
    private jakarta.servlet.http.HttpSession session;

    public void addToCart(Long productId) {
        // Check if item already exists in cart
        for (CartItem item : cartItems) {
            if (item.getProductId().equals(productId)) {
                int newQty = item.getQuantity() + 1;
                // Attempt to reserve the extra stock if it's a promo item
                int reservedQty = reservationService.reservePromoStock(session.getId(), productId, newQty);
                item.setQuantity(newQty);
                item.setReservedPromoQty(reservedQty); // We need to add this property to CartItem
                return;
            }
        }

        // If not exists, fetch product from DB and add to cart
        Optional<Product> optionalProduct = productRepository.findById(productId);
        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();
            CartItem cartItem = new CartItem();
            cartItem.setProductId(product.getId());
            cartItem.setName(product.getName());
            cartItem.setPrice(product.getPrice());
            cartItem.setImageUrl(product.getImageUrl());
            cartItem.setQuantity(1);
            cartItem.setIsDiscount(product.getDiscount());
            cartItem.setPromoPrice(product.getPromoPrice());
            cartItem.setPromoQuantity(product.getPromoQuantity());
            
            // Attempt reservation for 1 item
            int reservedQty = reservationService.reservePromoStock(session.getId(), product.getId(), 1);
            cartItem.setReservedPromoQty(reservedQty);

            cartItems.add(cartItem);
        }
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void removeFromCart(Long productId) {
        cartItems.removeIf(item -> item.getProductId().equals(productId));
        reservationService.releaseReservation(session.getId(), productId);
    }

    public void updateCartQuantity(Long productId, int quantity) {
        for (CartItem item : cartItems) {
            if (item.getProductId().equals(productId)) {
                int reservedQty = reservationService.reservePromoStock(session.getId(), productId, quantity);
                item.setQuantity(quantity);
                item.setReservedPromoQty(reservedQty);
                return;
            }
        }
    }

    public void clearCart() {
        cartItems.clear();
        reservationService.releaseAllReservations(session.getId());
    }
    
    public double getTotalPrice() {
        // Sync reservation statuses dynamically before calculating total
        for (CartItem item : cartItems) {
             int activeReserved = reservationService.getActiveReservationQuantity(session.getId(), item.getProductId());
             item.setReservedPromoQty(activeReserved);
        }

        return cartItems.stream()
            .mapToDouble(CartItem::getItemTotalPrice)
            .sum();
    }
    
    public int getTotalItems() {
        return cartItems.stream()
            .mapToInt(CartItem::getQuantity)
            .sum();
    }

    /**
     * Shipping fee logic:
     * Free ship if subtotal >= 1,000,000 VND AND total quantity >= 2
     * Otherwise: 30,000 VND
     */
    public double getShippingFee() {
        double subtotal = getTotalPrice();
        int totalItems = getTotalItems();
        if (subtotal >= 1_000_000 && totalItems >= 2) {
            return 0;
        }
        return 30_000;
    }

    public double getGrandTotal() {
        return getTotalPrice() + getShippingFee();
    }
}
