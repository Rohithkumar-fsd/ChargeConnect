package com.example.chargeconnect.discovery.dto;

import com.example.chargeconnect.charger.dto.AvailabilityStatus;
import com.example.chargeconnect.charger.model.LocationType;
import com.example.chargeconnect.charger.model.PowerType;
import com.example.chargeconnect.discovery.repo.DiscoveredChargerProjection;
import com.example.chargeconnect.vehicle.model.ConnectorType;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public record DiscoveredChargerResponse(
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
        List<ConnectorType> connectorTypes,
        Double distanceMeters
) {
    public static DiscoveredChargerResponse fromProjection(DiscoveredChargerProjection p) {
        List<ConnectorType> connectors = p.getConnectorTypesCsv() != null
                ? Arrays.stream(p.getConnectorTypesCsv().split(","))
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .map(ConnectorType::valueOf)
                .sorted(Comparator.comparing(Enum::name))
                .toList()
                : Collections.emptyList();

        return new DiscoveredChargerResponse(
                p.getId(),
                p.getHostId(),
                p.getHostName(),
                p.getTitle(),
                p.getDescription(),
                p.getLocationType() != null ? LocationType.valueOf(p.getLocationType()) : null,
                p.getAddressLine(),
                p.getCity(),
                p.getState(),
                p.getPostalCode(),
                p.getCountry(),
                p.getLatitude(),
                p.getLongitude(),
                p.getPowerType() != null ? PowerType.valueOf(p.getPowerType()) : null,
                p.getMaxPowerKw(),
                p.getPricePerKwh(),
                p.getAvailabilityStatus() != null ? AvailabilityStatus.valueOf(p.getAvailabilityStatus()) : null,
                connectors,
                p.getDistanceMeters()
        );
    }
}

