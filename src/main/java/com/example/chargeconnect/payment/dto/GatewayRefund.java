package com.example.chargeconnect.payment.dto;

import java.math.BigDecimal;

public record GatewayRefund(
        String refundId,
        BigDecimal amount,
        String status
) {}