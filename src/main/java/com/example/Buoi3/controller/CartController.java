package com.example.Buoi3.controller;

import com.example.Buoi3.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public String viewCart(Model model) {
        model.addAttribute("cartItems", cartService.getCartItems());
        model.addAttribute("totalPrice", cartService.getTotalPrice());
        return "products/cart"; // View we will create next
    }

    @GetMapping("/add/{id}")
    public String addToCart(@PathVariable("id") Long productId) {
        cartService.addToCart(productId);
        return "redirect:/cart";
    }

    @PostMapping("/update/{id}")
    public String updateCart(@PathVariable("id") Long productId, @RequestParam("quantity") int quantity) {
        if (quantity <= 0) {
            cartService.removeFromCart(productId);
        } else {
            cartService.updateCartQuantity(productId, quantity);
        }
        return "redirect:/cart";
    }

    @GetMapping("/remove/{id}")
    public String removeFromCart(@PathVariable("id") Long productId) {
        cartService.removeFromCart(productId);
        return "redirect:/cart";
    }

    @GetMapping("/clear")
    public String clearCart() {
        cartService.clearCart();
        return "redirect:/cart";
    }
}
