package com.example.chargeconnect.payout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectPayoutRequest(
        @NotBlank(message = "Rejection reason is required")
        @Size(max = 255, message = "Rejection reason cannot exceed 255 characters")
        String reason
) {}
