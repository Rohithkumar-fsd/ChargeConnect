package com.example.chargeconnect.discovery.repo;

import java.math.BigDecimal;

public interface DiscoveredChargerProjection {
    Long getId();
    Long getHostId();
    String getHostName();
    String getTitle();
    String getDescription();
    String getLocationType();
    String getAddressLine();
    String getCity();
    String getState();
    String getPostalCode();
    String getCountry();
    BigDecimal getLatitude();
    BigDecimal getLongitude();
    String getPowerType();
    BigDecimal getMaxPowerKw();
    BigDecimal getPricePerKwh();
    String getAvailabilityStatus();
    Double getDistanceMeters();
    String getConnectorTypesCsv();
}

