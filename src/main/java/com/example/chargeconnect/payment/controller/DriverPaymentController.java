package com.example.chargeconnect.payment.controller;

import com.example.chargeconnect.payment.dto.*;
import com.example.chargeconnect.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver/payments")
public class DriverPaymentController {

    private final PaymentService paymentService;

    public DriverPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Step 1: Create an order with Razorpay for a pending booking.
     */
    @PostMapping("/create-order")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentOrderResponse createPaymentOrder(
            Authentication authentication,
            @Valid @RequestBody CreatePaymentOrderRequest request
    ) {
        return paymentService.createPaymentOrder(authentication.getName(), request);
    }

    /**
     * Step 2: Submit payment ID and HMAC signature to verify payment and confirm booking.
     */
    @PostMapping("/verify")
    public PaymentResponse verifyPayment(
            Authentication authentication,
            @Valid @RequestBody VerifyPaymentRequest request
    ) {
        return paymentService.verifyPayment(authentication.getName(), request);
    }

    /**
     * Fetch payment receipt for a specific booking.
     */
    @GetMapping("/booking/{bookingId}")
    public PaymentResponse getPaymentByBooking(
            Authentication authentication,
            @PathVariable Long bookingId
    ) {
        return paymentService.getPaymentByBooking(authentication.getName(), bookingId);
    }

    /**
     * List all payment transactions for the authenticated driver.
     */
    @GetMapping
    public List<PaymentResponse> getMyPayments(Authentication authentication) {
        return paymentService.getDriverPayments(authentication.getName());
    }

    /**
     * Request a refund for a paid, eligible booking before it starts.
     */
    @PostMapping("/booking/{bookingId}/refund")
    public PaymentResponse refundPayment(
            Authentication authentication,
            @PathVariable Long bookingId,
            @RequestParam(required = false) String reason
    ) {
        return paymentService.processRefund(authentication.getName(), bookingId, reason);
    }
}