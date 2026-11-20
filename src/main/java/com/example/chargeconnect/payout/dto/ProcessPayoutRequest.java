package com.example.chargeconnect.payout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProcessPayoutRequest(
        @NotBlank(message = "Transaction reference (e.g. Bank UTR / Reference ID) is required")
        @Size(max = 100, message = "Transaction reference cannot exceed 100 characters")
        String transactionReference
) {}
