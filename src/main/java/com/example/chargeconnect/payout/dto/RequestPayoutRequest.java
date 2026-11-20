package com.example.chargeconnect.payout.dto;

import com.example.chargeconnect.payout.model.PayoutMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RequestPayoutRequest(
        @NotNull(message = "Payout amount is required")
        @DecimalMin(value = "50.00", message = "Minimum payout amount is 50.00 INR")
        BigDecimal amount,

        @NotNull(message = "Payout method is required (UPI or BANK_TRANSFER)")
        PayoutMethod payoutMethod,

        @NotBlank(message = "Account details are required (e.g., UPI ID or Bank details)")
        @Size(max = 255, message = "Account details cannot exceed 255 characters")
        String accountDetails
) {}
