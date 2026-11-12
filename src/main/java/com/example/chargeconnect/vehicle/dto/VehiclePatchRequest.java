package com.example.chargeconnect.vehicle.dto;

import com.example.chargeconnect.vehicle.ConnectorType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Set;

public record VehiclePatchRequest(
        @Size(max = 100, message = "Manufacturer must be at most 100 characters")
        String manufacturer,

        @Size(max = 100, message = "Model must be at most 100 characters")
        String model,

        @Size(max = 30, message = "Registration number must be at most 30 characters")
        String registrationNumber,

        @DecimalMin(value = "0.0", inclusive = false, message = "Battery capacity must be greater than 0")
        @Digits(integer = 6, fraction = 2, message = "Battery capacity supports up to 6 integer digits and 2 decimal places")
        BigDecimal batteryCapacityKwh,

        @DecimalMin(value = "0.0", inclusive = false, message = "Max AC power must be greater than 0")
        @Digits(integer = 4, fraction = 2, message = "Max AC power supports up to 4 integer digits and 2 decimal places")
        BigDecimal maxAcPowerKw,

        @DecimalMin(value = "0.0", inclusive = false, message = "Max DC power must be greater than 0")
        @Digits(integer = 4, fraction = 2, message = "Max DC power supports up to 4 integer digits and 2 decimal places")
        BigDecimal maxDcPowerKw,

        Set<@NotNull(message = "Connector type cannot be null") ConnectorType> connectorTypes
) {}

