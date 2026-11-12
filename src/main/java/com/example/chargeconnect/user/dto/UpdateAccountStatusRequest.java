package com.example.chargeconnect.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @NotNull(message = "Account status is required")
        AccountStatus status
) {}

