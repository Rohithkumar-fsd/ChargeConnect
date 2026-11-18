package com.example.chargeconnect.session.dto;

import com.example.chargeconnect.session.model.ChargingSession;
import com.example.chargeconnect.session.model.SessionStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

public record LiveTelemetryResponse(
        Long sessionId,
        String sessionReference,
        SessionStatus status,
        Instant startTime,
        long elapsedSeconds,
        long elapsedMinutes,
        BigDecimal currentPowerKw,
        BigDecimal energyDeliveredKwh,
        BigDecimal pricePerKwh,
        BigDecimal currentCost,
        BigDecimal batteryCapacityKwh,
        Double estimatedBatteryPercent
) {
    public static LiveTelemetryResponse from(ChargingSession session) {
        Instant now = session.getEndTime() != null ? session.getEndTime() : Instant.now();
        long seconds = Math.max(0, Duration.between(session.getStartTime(), now).getSeconds());
        long minutes = seconds / 60;

        BigDecimal energy = session.getStatus() == SessionStatus.ACTIVE
                ? session.calculateDeliveredEnergy(now)
                : session.getEnergyDeliveredKwh();

        BigDecimal cost = energy.multiply(session.getPricePerKwh()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal batteryCap = session.getVehicle() != null ? session.getVehicle().getBatteryCapacityKwh() : null;

        Double batteryPercent = null;
        if (batteryCap != null && batteryCap.compareTo(BigDecimal.ZERO) > 0) {
            batteryPercent = Math.min(100.0, energy.divide(batteryCap, 4, RoundingMode.HALF_UP).doubleValue() * 100.0);
        }

        return new LiveTelemetryResponse(
                session.getId(),
                session.getSessionReference(),
                session.getStatus(),
                session.getStartTime(),
                seconds,
                minutes,
                session.getCharger().getMaxPowerKw(),
                energy,
                session.getPricePerKwh(),
                cost,
                batteryCap,
                batteryPercent
        );
    }
}