package com.example.chargeconnect.vehicle.dto;

import com.example.chargeconnect.vehicle.ConnectorType;
import com.example.chargeconnect.vehicle.Vehicle;
import com.example.chargeconnect.vehicle.VehicleConnector;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record VehicleResponse(
        Long id,
        String manufacturer,
        String model,
        String registrationNumber,
        BigDecimal batteryCapacityKwh,
        BigDecimal maxAcPowerKw,
        BigDecimal maxDcPowerKw,
        List<ConnectorType> connectorTypes,
        Instant createdAt,
        Instant updatedAt
) {
    public static VehicleResponse from(Vehicle vehicle) {
        List<ConnectorType> connectors = vehicle.getConnectors().stream()
                .map(VehicleConnector::getConnectorType)
                .sorted(Comparator.comparing(Enum::name))
                .toList();

        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getManufacturer(),
                vehicle.getModel(),
                vehicle.getRegistrationNumber(),
                vehicle.getBatteryCapacityKwh(),
                vehicle.getMaxAcPowerKw(),
                vehicle.getMaxDcPowerKw(),
                connectors,
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }
}

