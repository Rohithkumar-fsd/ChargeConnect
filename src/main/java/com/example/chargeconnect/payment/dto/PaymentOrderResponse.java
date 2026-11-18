package com.example.chargeconnect.payment.dto;

import java.math.BigDecimal;

public record PaymentOrderResponse(
        Long paymentId,
        Long bookingId,
        String razorpayOrderId,
        BigDecimal amount,
        String currency,
        String keyId
) {}