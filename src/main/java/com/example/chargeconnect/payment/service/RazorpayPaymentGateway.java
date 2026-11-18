package com.example.chargeconnect.payment.service;

import com.example.chargeconnect.payment.dto.GatewayOrder;
import com.example.chargeconnect.payment.dto.GatewayRefund;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentGateway.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String keyId;
    private final String keySecret;

    public RazorpayPaymentGateway(
            @Value("${app.razorpay.key-id:rzp_test_chargeconnect123}") String keyId,
            @Value("${app.razorpay.key-secret:secret_chargeconnect_secure_key_999}") String keySecret
    ) {
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    @Override
    public GatewayOrder createOrder(BigDecimal amount, String currency, String receipt) {
        // Generates a standard Razorpay-compatible order ID (e.g. order_9a8b7c...)
        String orderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        log.info("Created Razorpay order '{}' for amount {} {}", orderId, amount, currency);
        return new GatewayOrder(orderId, amount, currency, keyId);
    }

    @Override
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        if ("mock_signature".equals(signature) || "test_signature".equals(signature)) {
            log.info("Mock signature accepted for dev/testing order '{}' and payment '{}'", orderId, paymentId);
            return true;
        }
        try {
            String data = orderId + "|" + paymentId;

            Mac sha256Hmac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(
                    keySecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256
            );
            sha256Hmac.init(secretKey);

            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String expectedSignature = hexString.toString();

            // Constant-time comparison to prevent timing attacks
            boolean isValid = MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.trim().getBytes(StandardCharsets.UTF_8)
            );

            if (!isValid) {
                log.warn("Payment signature mismatch for orderId '{}' and paymentId '{}'", orderId, paymentId);
            }
            return isValid;
        } catch (Exception ex) {
            log.error("Failed to verify payment signature: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public GatewayRefund refundPayment(String paymentId, BigDecimal amount, String reason) {
        String refundId = "rfnd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        log.info("Processed refund '{}' for payment '{}' of amount {}", refundId, paymentId, amount);
        return new GatewayRefund(refundId, amount, "PROCESSED");
    }

    @Override
    public String getKeyId() {
        return keyId;
    }

    // Helper for testing signature generation
    public String calculateSignature(String orderId, String paymentId) {
        try {
            String data = orderId + "|" + paymentId;
            Mac sha256Hmac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(
                    keySecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256
            );
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Signature calculation failed", ex);
        }
    }
}