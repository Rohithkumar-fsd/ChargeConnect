package com.example.chargeconnect.discovery.dto;

import com.example.chargeconnect.vehicle.model.ConnectorType;
import java.math.BigDecimal;
import java.util.List;

public record VehicleMatchedChargerResponse(
        DiscoveredChargerResponse charger,
        Long vehicleId,
        List<ConnectorType> matchedConnectors,
        BigDecimal effectivePowerKw
) {}

