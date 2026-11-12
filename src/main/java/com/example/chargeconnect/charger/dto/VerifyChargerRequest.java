package com.example.chargeconnect.charger.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerifyChargerRequest(
        @NotNull(message = "Verification decision is required")
        Decision decision,

        @Size(max = 500, message = "Rejection reason must be at most 500 characters")
        String rejectionReason
) {
    public enum Decision {
        APPROVE,
        REJECT
    }
}

