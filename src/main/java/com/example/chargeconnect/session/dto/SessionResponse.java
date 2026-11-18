package com.example.chargeconnect.session.dto;

import com.example.chargeconnect.session.model.ChargingSession;
import com.example.chargeconnect.session.model.SessionStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record SessionResponse(
        Long id,
        String sessionReference,
        Long bookingId,
        Long driverId,
        Long chargerId,
        String chargerTitle,
        Long vehicleId,
        SessionStatus status,
        Instant startTime,
        Instant endTime,
        BigDecimal initialMeterKwh,
        BigDecimal finalMeterKwh,
        BigDecimal energyDeliveredKwh,
        BigDecimal pricePerKwh,
        BigDecimal totalCost,
        String stopReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static SessionResponse from(ChargingSession session) {
        return new SessionResponse(
                session.getId(),
                session.getSessionReference(),
                session.getBooking().getId(),
                session.getDriver().getId(),
                session.getCharger().getId(),
                session.getCharger().getTitle(),
                session.getVehicle() != null ? session.getVehicle().getId() : null,
                session.getStatus(),
                session.getStartTime(),
                session.getEndTime(),
                session.getInitialMeterKwh(),
                session.getFinalMeterKwh(),
                session.getEnergyDeliveredKwh(),
                session.getPricePerKwh(),
                session.getTotalCost(),
                session.getStopReason(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}