package com.example.chargeconnect.charger.dto;

import com.example.chargeconnect.charger.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAvailabilityRequest(
        @NotNull(message = "Availability status is required")
        AvailabilityStatus availabilityStatus
) {}

