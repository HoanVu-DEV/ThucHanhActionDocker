package com.example.Buoi3.controller;

import com.example.Buoi3.Entity.User;
import com.example.Buoi3.repository.UserRepository;
import com.example.Buoi3.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/points")
@RequiredArgsConstructor
public class PointsController {

    private final UserRepository userRepository;
    private final OtpService otpService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    /** Trang đổi điểm - chỉ USER */
    @GetMapping("/redeem")
    public String redeemPage(Model model) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/login";

        int points = user.getRewardPoints() != null ? user.getRewardPoints() : 0;
        int maxPairs = points / 2;
        double maxDiscount = maxPairs * 15000.0;

        model.addAttribute("user", user);
        model.addAttribute("points", points);
        model.addAttribute("maxPairs", maxPairs);
        model.addAttribute("maxDiscount", maxDiscount);
        model.addAttribute("otpSent", false);
        return "points/redeem";
    }

    /** Bước 1: Gửi OTP về email */
    @PostMapping("/send-otp")
    public String sendOtp(@RequestParam("redeemPairs") int redeemPairs,
                          RedirectAttributes ra) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/login";

        int points = user.getRewardPoints() != null ? user.getRewardPoints() : 0;
        int maxPairs = points / 2;

        if (redeemPairs <= 0 || redeemPairs > maxPairs) {
            ra.addFlashAttribute("error", "Số điểm không hợp lệ!");
            return "redirect:/points/redeem";
        }

        try {
            otpService.generateAndSendOtp(user.getUsername(), user.getEmail());
            ra.addFlashAttribute("otpSent", true);
            ra.addFlashAttribute("redeemPairs", redeemPairs);
            ra.addFlashAttribute("success", "✅ Mã OTP đã được gửi tới email: " + user.getEmail());
            return "redirect:/points/verify";
        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "❌ Không thể gửi email OTP: " + e.getMessage());
            return "redirect:/points/redeem";
        }
    }

    /** Trang nhập OTP */
    @GetMapping("/verify")
    public String verifyPage(Model model) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/login";

        // If no OTP was sent (no flash attributes), redirect back to redeem
        if (!model.containsAttribute("redeemPairs")) {
            return "redirect:/points/redeem";
        }
        model.addAttribute("user", user);
        return "points/verify-otp";
    }

    /** Bước 2: Xác thực OTP và đổi điểm */
    @PostMapping("/confirm")
    public String confirmRedeem(@RequestParam("otp") String otp,
                                @RequestParam("redeemPairs") int redeemPairs,
                                RedirectAttributes ra) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/login";

        boolean valid = otpService.validateOtp(user.getUsername(), otp);
        if (!valid) {
            ra.addFlashAttribute("error", "Mã OTP không đúng hoặc đã hết hạn!");
            ra.addFlashAttribute("otpSent", true);
            ra.addFlashAttribute("redeemPairs", redeemPairs);
            return "redirect:/points/verify";
        }

        // Deduct points
        int currentPoints = user.getRewardPoints() != null ? user.getRewardPoints() : 0;
        int pointsToUse = redeemPairs * 2;
        if (pointsToUse > currentPoints) pointsToUse = currentPoints;

        double discountValue = (pointsToUse / 2) * 15000.0;
        user.setRewardPoints(currentPoints - pointsToUse);
        userRepository.save(user);

        ra.addFlashAttribute("successMsg",
            "Đổi điểm thành công! Bạn đã dùng " + pointsToUse + " điểm để nhận giảm giá " +
            String.format("%,.0f", discountValue) + "₫. Điểm còn lại: " + user.getRewardPoints());
        return "redirect:/points/redeem";
    }
}
