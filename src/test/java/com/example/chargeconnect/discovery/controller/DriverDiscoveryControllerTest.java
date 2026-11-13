package com.example.chargeconnect.discovery.controller;

import com.example.chargeconnect.charger.dto.AvailabilityStatus;
import com.example.chargeconnect.charger.model.LocationType;
import com.example.chargeconnect.charger.model.PowerType;
import com.example.chargeconnect.discovery.dto.DiscoveredChargerResponse;
import com.example.chargeconnect.discovery.dto.VehicleMatchedChargerResponse;
import com.example.chargeconnect.discovery.service.DiscoveryService;
import com.example.chargeconnect.exception.GlobalExceptionHandler;
import com.example.chargeconnect.exception.ResourceNotFoundException;
import com.example.chargeconnect.vehicle.model.ConnectorType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DriverDiscoveryControllerTest {

    @Mock
    private DiscoveryService discoveryService;

    private MockMvc mockMvc;
    private Authentication driverAuth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DriverDiscoveryController(discoveryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        driverAuth = new UsernamePasswordAuthenticationToken("asha@example.com", null);
    }

    @Test
    void findNearbyForVehicleReturnsOk() throws Exception {
        DiscoveredChargerResponse charger = sampleCharger();
        VehicleMatchedChargerResponse matched = new VehicleMatchedChargerResponse(
                charger, 101L, List.of(ConnectorType.CCS2), new BigDecimal("50.00")
        );

        when(discoveryService.findCompatibleChargersForVehicle(
                eq("asha@example.com"), eq(101L), eq(12.9716), eq(77.5946), eq(5000.0), any()
        )).thenReturn(List.of(matched));

        mockMvc.perform(get("/api/driver/discovery/nearby")
                        .principal(driverAuth)
                        .param("vehicleId", "101")
                        .param("latitude", "12.9716")
                        .param("longitude", "77.5946")
                        .param("radiusMeters", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value(101))
                .andExpect(jsonPath("$[0].matchedConnectors[0]").value("CCS2"))
                .andExpect(jsonPath("$[0].effectivePowerKw").value(50.00))
                .andExpect(jsonPath("$[0].charger.distanceMeters").value(1200.0));
    }

    @Test
    void findCompatibleNearbyReturnsNotFoundWhenVehicleMissing() throws Exception {
        when(discoveryService.findCompatibleChargersForVehicle(
                eq("asha@example.com"), eq(999L), eq(12.9716), eq(77.5946), any(), any()
        )).thenThrow(new ResourceNotFoundException("Vehicle not found"));

        mockMvc.perform(get("/api/driver/discovery/nearby")
                        .principal(driverAuth)
                        .param("vehicleId", "999")
                        .param("latitude", "12.9716")
                        .param("longitude", "77.5946"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findCompatibleNearbyReturnsBadRequestWhenMissingCoordinates() throws Exception {
        mockMvc.perform(get("/api/driver/discovery/nearby")
                        .principal(driverAuth)
                        .param("vehicleId", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private DiscoveredChargerResponse sampleCharger() {
        return new DiscoveredChargerResponse(
                1L, 20L, "Host Person", "Fast Hub", "Parking",
                LocationType.COMMERCIAL, "MG Road", "Bengaluru", "Karnataka", "560001", "India",
                new BigDecimal("12.971600"), new BigDecimal("77.594600"),
                PowerType.DC, new BigDecimal("60.00"), new BigDecimal("22.00"),
                AvailabilityStatus.AVAILABLE, List.of(ConnectorType.CCS2), 1200.0
        );
    }
}
