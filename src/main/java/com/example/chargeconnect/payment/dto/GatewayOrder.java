package com.example.chargeconnect.payment.dto;

import java.math.BigDecimal;

public record GatewayOrder(
        String orderId,
        BigDecimal amount,
        String currency,
        String keyId
) {}