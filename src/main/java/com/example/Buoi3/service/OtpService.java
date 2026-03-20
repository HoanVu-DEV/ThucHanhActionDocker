package com.example.Buoi3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final JavaMailSender mailSender;

    // Store OTP per username: username -> {otp, expiryTime}
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    private static final long OTP_VALIDITY_MS = 5 * 60 * 1000; // 5 phút

    public void generateAndSendOtp(String username, String toEmail) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        long expiry = System.currentTimeMillis() + OTP_VALIDITY_MS;
        otpStore.put(username, new OtpEntry(otp, expiry));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("chatter339@gmail.com");
        message.setTo(toEmail);
        message.setSubject("🎁 [TGDD] Mã OTP đổi điểm thưởng của bạn");
        message.setText(
            "Xin chào " + username + ",\n\n" +
            "Bạn đã yêu cầu đổi điểm thưởng tại Thế Giới Di Động.\n\n" +
            "Mã OTP của bạn là: " + otp + "\n\n" +
            "Mã này có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này với ai.\n\n" +
            "Nếu bạn không yêu cầu, hãy bỏ qua email này.\n\n" +
            "Trân trọng,\nThế Giới Di Động"
        );
        mailSender.send(message);
    }

    public boolean validateOtp(String username, String inputOtp) {
        OtpEntry entry = otpStore.get(username);
        if (entry == null) return false;
        if (System.currentTimeMillis() > entry.expiry()) {
            otpStore.remove(username);
            return false;
        }
        boolean valid = entry.otp().equals(inputOtp.trim());
        if (valid) otpStore.remove(username); // consume OTP
        return valid;
    }

    public void clearOtp(String username) {
        otpStore.remove(username);
    }

    private record OtpEntry(String otp, long expiry) {}
}
