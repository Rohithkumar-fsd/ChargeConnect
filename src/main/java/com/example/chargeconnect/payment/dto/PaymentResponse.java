package com.example.chargeconnect.payment.dto;

import com.example.chargeconnect.payment.model.Payment;
import com.example.chargeconnect.payment.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long bookingId,
        String bookingReference,
        Long driverId,
        String razorpayOrderId,
        String razorpayPaymentId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getBooking().getId(),
                p.getBooking().getBookingReference(),
                p.getDriver().getId(),
                p.getRazorpayOrderId(),
                p.getRazorpayPaymentId(),
                p.getAmount(),
                p.getCurrency(),
                p.getStatus(),
                p.getFailureReason(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}