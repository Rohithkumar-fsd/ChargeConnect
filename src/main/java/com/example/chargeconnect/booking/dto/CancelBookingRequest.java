package com.example.chargeconnect.booking.dto;

import jakarta.validation.constraints.Size;

public record CancelBookingRequest(
        @Size(max = 255, message = "Cancellation reason must be at most 255 characters")
        String cancellationReason
) {}

