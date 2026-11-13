package com.example.chargeconnect.charger.dto;

import com.example.chargeconnect.charger.model.Charger;
import com.example.chargeconnect.charger.model.ChargerConnector;
import com.example.chargeconnect.charger.model.LocationType;
import com.example.chargeconnect.charger.model.PowerType;
import com.example.chargeconnect.vehicle.model.ConnectorType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

public record ChargerResponse(
        Long id,
        Long hostId,
        String hostName,
        String title,
        String description,
        LocationType locationType,
        String addressLine,
        String city,
        String state,
        String postalCode,
        String country,
        BigDecimal latitude,
        BigDecimal longitude,
        PowerType powerType,
        BigDecimal maxPowerKw,
        BigDecimal pricePerKwh,
        LocalTime openTime,
        LocalTime closeTime,
        AvailabilityStatus availabilityStatus,
        VerificationStatus verificationStatus,
        String rejectionReason,
        Long verifiedByUserId,
        Instant verifiedAt,
        List<ConnectorType> connectorTypes,
        Instant createdAt,
        Instant updatedAt
) {
    public ChargerResponse(
            Long id,
            Long hostId,
            String hostName,
            String title,
            String description,
            LocationType locationType,
            String addressLine,
            String city,
            String state,
            String postalCode,
            String country,
            BigDecimal latitude,
            BigDecimal longitude,
            PowerType powerType,
            BigDecimal maxPowerKw,
            BigDecimal pricePerKwh,
            AvailabilityStatus availabilityStatus,
            VerificationStatus verificationStatus,
            String rejectionReason,
            Long verifiedByUserId,
            Instant verifiedAt,
            List<ConnectorType> connectorTypes,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(
                id, hostId, hostName, title, description, locationType, addressLine, city, state, postalCode, country,
                latitude, longitude, powerType, maxPowerKw, pricePerKwh,
                LocalTime.of(0, 0, 0), LocalTime.of(23, 59, 59),
                availabilityStatus, verificationStatus, rejectionReason, verifiedByUserId, verifiedAt,
                connectorTypes, createdAt, updatedAt
        );
    }

    public static ChargerResponse from(Charger charger) {
        List<ConnectorType> connectors = charger.getConnectors().stream()
                .map(ChargerConnector::getConnectorType)
                .sorted(Comparator.comparing(Enum::name))
                .toList();

        return new ChargerResponse(
                charger.getId(),
                charger.getHost().getId(),
                charger.getHost().getFullName(),
                charger.getTitle(),
                charger.getDescription(),
                charger.getLocationType(),
                charger.getAddressLine(),
                charger.getCity(),
                charger.getState(),
                charger.getPostalCode(),
                charger.getCountry(),
                charger.getLatitude(),
                charger.getLongitude(),
                charger.getPowerType(),
                charger.getMaxPowerKw(),
                charger.getPricePerKwh(),
                charger.getOpenTime(),
                charger.getCloseTime(),
                charger.getAvailabilityStatus(),
                charger.getVerificationStatus(),
                charger.getRejectionReason(),
                charger.getVerifiedBy() != null ? charger.getVerifiedBy().getId() : null,
                charger.getVerifiedAt(),
                connectors,
                charger.getCreatedAt(),
                charger.getUpdatedAt()
        );
    }
}

