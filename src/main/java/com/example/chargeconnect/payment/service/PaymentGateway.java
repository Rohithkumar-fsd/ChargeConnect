package com.example.chargeconnect.payment.service;

import com.example.chargeconnect.payment.dto.GatewayOrder;
import com.example.chargeconnect.payment.dto.GatewayRefund;
import java.math.BigDecimal;

public interface PaymentGateway {

    /**
     * Creates an order with the gateway before user pays.
     */
    GatewayOrder createOrder(BigDecimal amount, String currency, String receipt);

    /**
     * Verifies that the cryptographic signature returned by the gateway is genuine.
     */
    boolean verifySignature(String orderId, String paymentId, String signature);

    /**
     * Requests a refund from the gateway for a captured payment.
     */
    GatewayRefund refundPayment(String paymentId, BigDecimal amount, String reason);

    /**
     * Returns the public Key ID needed by the mobile checkout modal.
     */
    String getKeyId();
}