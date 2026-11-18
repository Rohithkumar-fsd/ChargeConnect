package com.example.chargeconnect.session.dto;

import jakarta.validation.constraints.NotBlank;

public record StartSessionRequest(
        Long bookingId,

        @NotBlank(message = "Charger QR code is required")
        String qrCode
) {}