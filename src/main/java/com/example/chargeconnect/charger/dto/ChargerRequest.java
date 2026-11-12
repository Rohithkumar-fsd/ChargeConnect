package com.example.chargeconnect.charger.dto;

import com.example.chargeconnect.charger.LocationType;
import com.example.chargeconnect.charger.PowerType;
import com.example.chargeconnect.vehicle.ConnectorType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Set;

public record ChargerRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title must be at most 120 characters")
        String title,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description,

        @NotNull(message = "Location type is required")
        LocationType locationType,

        @NotBlank(message = "Address line is required")
        @Size(max = 255, message = "Address line must be at most 255 characters")
        String addressLine,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State must be at most 100 characters")
        String state,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code must be at most 20 characters")
        String postalCode,

        @NotBlank(message = "Country is required")
        @Size(max = 80, message = "Country must be at most 80 characters")
        String country,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        @Digits(integer = 3, fraction = 6, message = "Latitude supports up to 3 integer digits and 6 decimal places")
        BigDecimal latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        @Digits(integer = 3, fraction = 6, message = "Longitude supports up to 3 integer digits and 6 decimal places")
        BigDecimal longitude,

        @NotNull(message = "Power type is required")
        PowerType powerType,

        @NotNull(message = "Max power is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Max power must be greater than 0")
        @Digits(integer = 4, fraction = 2, message = "Max power supports up to 4 integer digits and 2 decimal places")
        BigDecimal maxPowerKw,

        @NotNull(message = "Price per kWh is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price per kWh must be greater than 0")
        @Digits(integer = 6, fraction = 2, message = "Price per kWh supports up to 6 integer digits and 2 decimal places")
        BigDecimal pricePerKwh,

        @NotEmpty(message = "At least one connector type is required")
        Set<@NotNull(message = "Connector type cannot be null") ConnectorType> connectorTypes
) {}

