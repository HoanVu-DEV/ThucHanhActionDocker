package com.example.Buoi3.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MomoService {

    // MoMo Sandbox / Test credentials
    private final String endpoint = "https://test-payment.momo.vn/v2/gateway/api/create";
    private final String partnerCode = "MOMOBKUN20180529";
    private final String accessKey = "klm05TvNBzhg7h7j";
    private final String secretKey = "at67qH6mk8w5Y1nAyMoYKMWACiEi2bsa";

    /**
     * Create a MoMo payment request and return the payUrl for redirect.
     *
     * @param orderId   The internal order ID from our system
     * @param amount    The total amount in VND (long value, e.g. 1500000)
     * @param orderInfo Description shown to user on MoMo
     * @param baseUrl   The base URL of our site, e.g. http://localhost:8080
     * @return The payUrl to redirect the user to MoMo's payment page
     */
    public String createPayment(Long orderId, long amount, String orderInfo, String baseUrl) throws Exception {
        String momoOrderId = orderId + "_" + System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        String amountStr = String.valueOf(amount);
        String returnUrl = baseUrl + "/order/momo-return";
        String notifyUrl = baseUrl + "/order/momo-notify";
        String requestType = "captureWallet";
        String extraData = "";

        // Build raw signature string (params must be in ALPHABETICAL order)
        String rawSignature = "accessKey=" + accessKey +
                "&amount=" + amountStr +
                "&extraData=" + extraData +
                "&ipnUrl=" + notifyUrl +
                "&orderId=" + momoOrderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&redirectUrl=" + returnUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;

        String signature = hmacSHA256(rawSignature, secretKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("partnerCode", partnerCode);
        requestBody.put("partnerName", "Thế Giới Di Động");
        requestBody.put("storeId", "MomoTestStore");
        requestBody.put("requestId", requestId);
        requestBody.put("amount", Long.parseLong(amountStr));
        requestBody.put("orderId", momoOrderId);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("redirectUrl", returnUrl);
        requestBody.put("ipnUrl", notifyUrl);
        requestBody.put("lang", "vi");
        requestBody.put("extraData", extraData);
        requestBody.put("requestType", requestType);
        requestBody.put("signature", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(endpoint, entity, Map.class);

        System.out.println("MoMo Response: " + response);

        if (response != null && response.containsKey("payUrl")) {
            return (String) response.get("payUrl");
        }

        String errorMsg = response != null ? response.toString() : "No response from MoMo";
        throw new RuntimeException("MoMo payment failed: " + errorMsg);
    }

    private String hmacSHA256(String data, String key) throws Exception {
        Mac sha256HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256HMAC.init(secretKeySpec);
        byte[] hash = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}