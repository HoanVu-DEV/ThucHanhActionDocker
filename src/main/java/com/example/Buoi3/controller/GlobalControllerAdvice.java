package com.example.Buoi3.controller;

import com.example.Buoi3.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final CartService cartService;

    @ModelAttribute("cartCount")
    public int getCartCount() {
        try {
            return cartService.getTotalItems();
        } catch (Exception e) {
            return 0; // Fallback in case session cannot be created
        }
    }
}
