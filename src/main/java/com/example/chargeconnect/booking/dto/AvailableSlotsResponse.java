package com.example.chargeconnect.booking.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AvailableSlotsResponse(
        Long chargerId,
        LocalDate date,
        LocalTime openTime,
        LocalTime closeTime,
        List<TimeSlot> slots
) {}

