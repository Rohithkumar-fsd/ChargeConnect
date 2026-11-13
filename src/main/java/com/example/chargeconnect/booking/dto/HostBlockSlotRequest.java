package com.example.chargeconnect.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record HostBlockSlotRequest(
        @NotNull(message = "Start time is required")
        @Future(message = "Start time must be in the future")
        Instant startTime,

        @NotNull(message = "End time is required")
        @Future(message = "End time must be in the future")
        Instant endTime,

        @Size(max = 255, message = "Reason must be at most 255 characters")
        String reason
) {}

