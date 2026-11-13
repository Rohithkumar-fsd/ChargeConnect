package com.example.chargeconnect.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateBookingRequest(
        @NotNull(message = "Charger ID is required")
        Long chargerId,

        @NotNull(message = "Vehicle ID is required")
        Long vehicleId,

        @NotNull(message = "Start time is required")
        @Future(message = "Start time must be in the future")
        Instant startTime,

        @NotNull(message = "End time is required")
        @Future(message = "End time must be in the future")
        Instant endTime
) {}

