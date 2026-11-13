package com.example.chargeconnect.booking.dto;

import java.time.Instant;

public record TimeSlot(
        Instant startTime,
        Instant endTime,
        String status
) {}

