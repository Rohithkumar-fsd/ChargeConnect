package com.example.chargeconnect.charger.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAvailabilityRequest(
        @NotNull(message = "Availability status is required")
        AvailabilityStatus availabilityStatus
) {}

