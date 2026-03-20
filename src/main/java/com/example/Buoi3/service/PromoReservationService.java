package com.example.Buoi3.service;

import com.example.Buoi3.Entity.Product;
import com.example.Buoi3.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global application-scoped service to handle 5-minute promotional limit reservations.
 * When a user adds a promo item to cart, it reserves stock here and deducts from DB immediately.
 * If they checkout, the reservation is successfully consumed.
 * If 5 minutes pass without checkout, the scheduled task restores the stock to the DB.
 */
@Service
@RequiredArgsConstructor
public class PromoReservationService {
    
    // Time to live for a cart reservation in minutes
    private static final int RESERVATION_MINUTES = 5;

    private final ProductRepository productRepository;

    // Outer map key: Session ID. Inner map key: Product ID, Value: Reservation details
    private final Map<String, Map<Long, Reservation>> reservations = new ConcurrentHashMap<>();

    // Inner record to hold state
    public static class Reservation {
        public Long productId;
        public int quantityReserved;
        public LocalDateTime reservedAt;

        public Reservation(Long productId, int quantityReserved) {
            this.productId = productId;
            this.quantityReserved = quantityReserved;
            this.reservedAt = LocalDateTime.now();
        }
    }

    /**
     * Attempt to reserve promo quantity for a given session.
     * Modifies DB to prevent others from booking the same items.
     */
    public synchronized int reservePromoStock(String sessionId, Long productId, int requestedQty) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null || product.getDiscount() == null || !product.getDiscount() || product.getPromoQuantity() == null || product.getPromoQuantity() <= 0) {
            return 0; // Not a valid promo
        }

        // Get session's current reservations
        Map<Long, Reservation> sessionRes = reservations.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());
        Reservation existingRes = sessionRes.get(productId);
        
        int currentReserved = existingRes != null ? existingRes.quantityReserved : 0;
        int additionalNeeded = requestedQty - currentReserved;

        if (additionalNeeded <= 0) {
            // They are decreasing their cart quantity or it's the same
            if (additionalNeeded < 0) {
                // Restore partly to DB
                product.setPromoQuantity(product.getPromoQuantity() + Math.abs(additionalNeeded));
                productRepository.save(product);
                existingRes.quantityReserved = requestedQty;
                // Update time so it doesn't expire while they're still active
                existingRes.reservedAt = LocalDateTime.now();
            }
            return requestedQty;
        }

        // Need more reservation stock
        int availableInDb = product.getPromoQuantity();
        int successfullyReserved = Math.min(additionalNeeded, availableInDb);
        
        if (successfullyReserved > 0) {
            // Deduct from DB
            product.setPromoQuantity(product.getPromoQuantity() - successfullyReserved);
            productRepository.save(product);
            
            // Add to session memory
            if (existingRes == null) {
                sessionRes.put(productId, new Reservation(productId, successfullyReserved));
            } else {
                existingRes.quantityReserved += successfullyReserved;
                existingRes.reservedAt = LocalDateTime.now();
            }
        }
        
        return currentReserved + successfullyReserved;
    }

    /**
     * Delete reservation when checkout is complete without restoring to DB
     * (since it was permanently bought).
     */
    public synchronized void consumeReservation(String sessionId) {
        reservations.remove(sessionId);
    }
    
    /**
     * Delete a single product reservation and RESTORE the DB stock.
     */
    public synchronized void releaseReservation(String sessionId, Long productId) {
        Map<Long, Reservation> sessionRes = reservations.get(sessionId);
        if (sessionRes != null) {
            Reservation res = sessionRes.remove(productId);
            if (res != null) {
                restoreToDb(res.productId, res.quantityReserved);
            }
            if (sessionRes.isEmpty()) {
                reservations.remove(sessionId);
            }
        }
    }
    
    /**
     * Release ALL reservations for a session and RESTORE the DB stock.
     */
    public synchronized void releaseAllReservations(String sessionId) {
        Map<Long, Reservation> sessionRes = reservations.remove(sessionId);
        if (sessionRes != null) {
            for (Reservation res : sessionRes.values()) {
                restoreToDb(res.productId, res.quantityReserved);
            }
        }
    }

    private void restoreToDb(Long productId, int quantity) {
        productRepository.findById(productId).ifPresent(product -> {
            Integer current = product.getPromoQuantity() != null ? product.getPromoQuantity() : 0;
            product.setPromoQuantity(current + quantity);
            
            // If they had discount turned off accidentally when it hit 0, we can turn it back on
            if (product.getPromoQuantity() > 0) {
                 product.setDiscount(true);
            }
            productRepository.save(product);
        });
    }

    /**
     * Background Job: Runs every 1 minute.
     * Finds and releases reservations older than 5 minutes.
     */
    @Scheduled(fixedRate = 60000) // 1 minute in milliseconds
    public synchronized void cleanupExpiredReservations() {
        LocalDateTime expiryThreshold = LocalDateTime.now().minusMinutes(RESERVATION_MINUTES);
        
        for (Map.Entry<String, Map<Long, Reservation>> sessionEntry : reservations.entrySet()) {
            String sessionId = sessionEntry.getKey();
            Map<Long, Reservation> sessionMap = sessionEntry.getValue();
            
            Iterator<Map.Entry<Long, Reservation>> it = sessionMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, Reservation> entry = it.next();
                Reservation res = entry.getValue();
                
                if (res.reservedAt.isBefore(expiryThreshold)) {
                    System.out.println("RESERVATION EXPIRED: Releasing " + res.quantityReserved + " items of Product " + res.productId + " for session " + sessionId);
                    restoreToDb(res.productId, res.quantityReserved);
                    it.remove();
                }
            }
            if (sessionMap.isEmpty()) {
                reservations.remove(sessionId);
            }
        }
    }
    
    /**
     * Get active reserved quantity for a specific cart row to accurately display prices.
     */
    public int getActiveReservationQuantity(String sessionId, Long productId) {
        Map<Long, Reservation> sessionRes = reservations.get(sessionId);
        if (sessionRes != null) {
            Reservation res = sessionRes.get(productId);
            if (res != null) {
                return res.quantityReserved;
            }
        }
        return 0;
    }
}
