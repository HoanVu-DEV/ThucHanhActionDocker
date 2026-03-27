package com.example.Buoi3.controller;

import com.example.Buoi3.Entity.User;
import com.example.Buoi3.repository.UserRepository;
import com.example.Buoi3.service.OtpService;
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

    @PostMapping("/send-otp")
    public String sendOtp(@RequestParam("redeemPairs") int redeemPairs, Model model) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/login";

        int points = user.getRewardPoints() != null ? user.getRewardPoints() : 0;
        int maxPairs = points / 2;

        if (redeemPairs <= 0 || redeemPairs > maxPairs) {
            model.addAttribute("error", "So diem khong hop le!");
            model.addAttribute("user", user);
            model.addAttribute("points", points);
            model.addAttribute("maxPairs", maxPairs);
            model.addAttribute("maxDiscount", maxPairs * 15000.0);
            model.addAttribute("otpSent", false);
            return "points/redeem";
        }

        try {
            otpService.generateAndSendOtp(user.getUsername(), user.getEmail());
            model.addAttribute("otpSent", true);
            model.addAttribute("redeemPairs", redeemPairs);
            model.addAttribute("success", "Ma OTP da duoc gui toi email: " + user.getEmail());
            model.addAttribute("user", user);
            return "points/verify-otp";
        } catch (Exception e) {
            model.addAttribute("error", "Khong the gui email OTP: " + e.getMessage());
            model.addAttribute("user", user);
            model.addAttribute("points", points);
            model.addAttribute("maxPairs", maxPairs);
            model.addAttribute("maxDiscount", maxPairs * 15000.0);
            model.addAttribute("otpSent", false);
            return "points/redeem";
        }
    }

    @GetMapping("/verify")
    public String verifyPage(Model model) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/login";
        if (!model.containsAttribute("redeemPairs")) {
            return "redirect:/points/redeem";
        }
        model.addAttribute("user", user);
        return "points/verify-otp";
    }

    @PostMapping("/confirm")
    public String confirmRedeem(@RequestParam("otp") String otp,
                                @RequestParam("redeemPairs") int redeemPairs,
                                Model model) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/login";

        boolean valid = otpService.validateOtp(user.getUsername(), otp);
        if (!valid) {
            model.addAttribute("error", "Ma OTP khong dung hoac da het han!");
            model.addAttribute("otpSent", true);
            model.addAttribute("redeemPairs", redeemPairs);
            model.addAttribute("user", user);
            return "points/verify-otp";
        }

        int currentPoints = user.getRewardPoints() != null ? user.getRewardPoints() : 0;
        int pointsToUse = redeemPairs * 2;
        if (pointsToUse > currentPoints) pointsToUse = currentPoints;

        double discountValue = (pointsToUse / 2) * 15000.0;
        user.setRewardPoints(currentPoints - pointsToUse);
        userRepository.save(user);

        model.addAttribute("successMsg",
            "Doi diem thanh cong! Ban da dung " + pointsToUse + " diem de nhan giam gia " +
            String.format("%,.0f", discountValue) + "d. Diem con lai: " + user.getRewardPoints());
        model.addAttribute("user", user);
        return "points/redeem";
    }
}
