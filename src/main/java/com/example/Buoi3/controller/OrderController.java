package com.example.Buoi3.controller;

import com.example.Buoi3.Entity.Order;
import com.example.Buoi3.Entity.User;
import com.example.Buoi3.repository.UserRepository;
import com.example.Buoi3.service.CartService;
import com.example.Buoi3.service.MomoService;
import com.example.Buoi3.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final MomoService momoService;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    @GetMapping("/checkout")
    public String checkoutForm(Model model) {
        if (cartService.getCartItems().isEmpty()) {
            return "redirect:/cart";
        }
        model.addAttribute("cartItems", cartService.getCartItems());
        model.addAttribute("totalPrice", cartService.getTotalPrice());
        model.addAttribute("shippingFee", cartService.getShippingFee());
        model.addAttribute("freeShip", cartService.getShippingFee() == 0);
        model.addAttribute("grandTotal", cartService.getGrandTotal());

        // Reward Points
        double subtotal = cartService.getTotalPrice();
        int earnablePoints = (int)(subtotal / 15000) * 2; // 2 điểm per 15,000₫
        model.addAttribute("earnablePoints", earnablePoints);

        User user = getCurrentUser();
        if (user != null) {
            int points = user.getRewardPoints() != null ? user.getRewardPoints() : 0;
            int maxRedeemPairs = points / 2; // cứ 2 điểm = 15k
            double maxDiscount = maxRedeemPairs * 15000.0;
            if (maxDiscount > cartService.getGrandTotal()) {
                maxDiscount = cartService.getGrandTotal();
                maxRedeemPairs = (int) Math.ceil(maxDiscount / 15000.0);
            }
            model.addAttribute("userPoints", points);
            model.addAttribute("maxRedeemPairs", maxRedeemPairs);
            model.addAttribute("pointValueVnd", 15000);
        } else {
            model.addAttribute("userPoints", 0);
            model.addAttribute("maxRedeemPairs", 0);
            model.addAttribute("pointValueVnd", 15000);
        }

        return "products/checkout";
    }

    @PostMapping("/submit")
    public String submitOrder(
            @RequestParam("customerName") String customerName,
            @RequestParam("phone") String phone,
            @RequestParam("address") String address,
            @RequestParam("notes") String notes,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(value = "redeemPairs", defaultValue = "0") int redeemPairs,
            HttpServletRequest request,
            Model model) {

        try {
            // Calculate points discount
            double pointsDiscount = redeemPairs * 15000.0;
            int pointsUsed = redeemPairs * 2;

            // Calculate final amount (grandTotal = subtotal + shipping)
            double subtotal = cartService.getTotalPrice(); // only product prices, NO shipping
            double grandTotal = cartService.getGrandTotal();
            double finalAmount = Math.max(0, grandTotal - pointsDiscount);

            // Deduct reward points from user
            User user = getCurrentUser();
            if (user != null && pointsUsed > 0) {
                int currentPoints = user.getRewardPoints() != null ? user.getRewardPoints() : 0;
                if (pointsUsed > currentPoints) {
                    pointsUsed = currentPoints; // safety guard
                }
                user.setRewardPoints(currentPoints - pointsUsed);
            }

            if ("MOMO".equalsIgnoreCase(paymentMethod)) {
                // ===== MoMo Payment Flow =====
                long totalAmount = Math.round(finalAmount);
                if (totalAmount <= 0) {
                    totalAmount = 1000;
                }

                Order order = orderService.createOrder(customerName, phone, address, notes, "MOMO");

                // Award new points: 2 points per 15,000₫ spent (subtotal only, NO shipping)
                if (user != null) {
                    int earnedPoints = (int)(subtotal / 15000) * 2;
                    int currentPts = user.getRewardPoints() != null ? user.getRewardPoints() : 0;
                    user.setRewardPoints(currentPts + earnedPoints);
                    userRepository.save(user);
                }

                String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
                String orderInfo = "Thanh toan don hang #" + order.getId();
                String payUrl = momoService.createPayment(order.getId(), totalAmount, orderInfo, baseUrl);

                return "redirect:" + payUrl;

            } else {
                // ===== COD / ATM / Other Payment Flow =====
                orderService.createOrder(customerName, phone, address, notes, paymentMethod);

                // Award new points: 2 points per 15,000₫ spent (subtotal only, NO shipping)
                if (user != null) {
                    int earnedPoints = (int)(subtotal / 15000) * 2;
                    int currentPts = user.getRewardPoints() != null ? user.getRewardPoints() : 0;
                    user.setRewardPoints(currentPts + earnedPoints);
                    userRepository.save(user);
                }

                return "redirect:/order/success";
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            return "redirect:/order/checkout?error=true";
        }
    }

    @GetMapping("/momo-return")
    public String momoReturn(
            @RequestParam(value = "resultCode", defaultValue = "-1") int resultCode,
            @RequestParam(value = "orderId", defaultValue = "") String momoOrderId,
            @RequestParam(value = "message", defaultValue = "") String message,
            Model model) {

        if (resultCode == 0) {
            return "redirect:/order/success?momo=true";
        } else {
            model.addAttribute("error", "Thanh toán MoMo thất bại: " + message);
            return "products/momo-failed";
        }
    }

    @PostMapping("/momo-notify")
    @org.springframework.web.bind.annotation.ResponseBody
    public String momoNotify(@org.springframework.web.bind.annotation.RequestBody String body) {
        System.out.println("MoMo IPN Notification: " + body);
        return "OK";
    }

    @GetMapping("/success")
    public String orderSuccess(@RequestParam(value = "momo", required = false) String momo, Model model) {
        if (momo != null) {
            model.addAttribute("momoSuccess", true);
        }
        // Show how many points the user now has
        User user = getCurrentUser();
        if (user != null) {
            model.addAttribute("userPoints", user.getRewardPoints() != null ? user.getRewardPoints() : 0);
        }
        return "products/order-success";
    }
}
