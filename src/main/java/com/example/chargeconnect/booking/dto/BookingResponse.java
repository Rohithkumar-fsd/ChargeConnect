package com.example.chargeconnect.booking.dto;

import com.example.chargeconnect.booking.model.Booking;
import com.example.chargeconnect.booking.model.BookingStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record BookingResponse(
        Long id,
        String bookingReference,
        Long driverId,
        String driverName,
        Long chargerId,
        String chargerTitle,
        Long vehicleId,
        String vehicleModel,
        Instant startTime,
        Instant endTime,
        BigDecimal totalHours,
        BigDecimal pricePerKwh,
        BigDecimal estimatedKwh,
        BigDecimal totalPrice,
        BookingStatus status,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static BookingResponse from(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getBookingReference(),
                b.getDriver().getId(),
                b.getDriver().getFullName(),
                b.getCharger().getId(),
                b.getCharger().getTitle(),
                b.getVehicle() != null ? b.getVehicle().getId() : null,
                b.getVehicle() != null ? b.getVehicle().getManufacturer() + " " + b.getVehicle().getModel() : null,
                b.getStartTime(),
                b.getEndTime(),
                b.getTotalHours(),
                b.getPricePerKwh(),
                b.getEstimatedKwh(),
                b.getTotalPrice(),
                b.getStatus(),
                b.getCancellationReason(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}

