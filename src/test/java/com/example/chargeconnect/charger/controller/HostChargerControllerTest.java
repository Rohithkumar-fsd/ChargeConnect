package com.example.chargeconnect.charger.controller;

import com.example.chargeconnect.charger.AvailabilityStatus;
import com.example.chargeconnect.charger.LocationType;
import com.example.chargeconnect.charger.PowerType;
import com.example.chargeconnect.charger.VerificationStatus;
import com.example.chargeconnect.charger.dto.ChargerResponse;
import com.example.chargeconnect.charger.service.ChargerService;
import com.example.chargeconnect.exception.GlobalExceptionHandler;
import com.example.chargeconnect.exception.ResourceNotFoundException;
import com.example.chargeconnect.vehicle.ConnectorType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HostChargerControllerTest {

    @Mock
    private ChargerService chargerService;

    private MockMvc mockMvc;
    private Authentication hostAuth;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new HostChargerController(chargerService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        hostAuth = new UsernamePasswordAuthenticationToken("host@example.com", null);
    }

    @Test
    void createChargerReturnsCreated() throws Exception {
        ChargerResponse response = sampleResponse(AvailabilityStatus.AVAILABLE, VerificationStatus.PENDING);
        when(chargerService.createCharger(eq("host@example.com"), any())).thenReturn(response);

        String body = """
                {
                  "title": "Indiranagar Wallbox",
                  "description": "Covered parking",
                  "locationType": "HOME",
                  "addressLine": "12th Main",
                  "city": "Bengaluru",
                  "state": "Karnataka",
                  "postalCode": "560038",
                  "country": "India",
                  "latitude": 12.971600,
                  "longitude": 77.594600,
                  "powerType": "AC",
                  "maxPowerKw": 7.40,
                  "pricePerKwh": 18.50,
                  "connectorTypes": ["TYPE_2"]
                }
                """;

        mockMvc.perform(post("/api/host/chargers")
                        .principal(hostAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"));
    }

    @Test
    void createChargerReturnsBadRequestForInvalidCoordinates() throws Exception {
        String invalidBody = """
                {
                  "title": "",
                  "locationType": "HOME",
                  "addressLine": "12th Main",
                  "city": "Bengaluru",
                  "state": "Karnataka",
                  "postalCode": "560038",
                  "country": "India",
                  "latitude": 120.0,
                  "longitude": 77.5946,
                  "powerType": "AC",
                  "maxPowerKw": 7.40,
                  "pricePerKwh": 18.50,
                  "connectorTypes": []
                }
                """;

        mockMvc.perform(post("/api/host/chargers")
                        .principal(hostAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.latitude").exists())
                .andExpect(jsonPath("$.fieldErrors.connectorTypes").exists());
    }

    @Test
    void getMyChargerReturnsNotFoundWhenMissing() throws Exception {
        when(chargerService.getMyCharger("host@example.com", 999L))
                .thenThrow(new ResourceNotFoundException("Charger not found"));

        mockMvc.perform(get("/api/host/chargers/999").principal(hostAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAvailabilityReturnsOk() throws Exception {
        ChargerResponse response = sampleResponse(AvailabilityStatus.UNAVAILABLE, VerificationStatus.APPROVED);
        when(chargerService.updateAvailability(eq("host@example.com"), eq(101L), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/host/chargers/101/availability")
                        .principal(hostAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilityStatus\":\"UNAVAILABLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availabilityStatus").value("UNAVAILABLE"));
    }

    @Test
    void deleteChargerReturnsNoContent() throws Exception {
        doNothing().when(chargerService).deleteCharger("host@example.com", 101L);

        mockMvc.perform(delete("/api/host/chargers/101").principal(hostAuth))
                .andExpect(status().isNoContent());
    }

    private ChargerResponse sampleResponse(AvailabilityStatus availability, VerificationStatus verification) {
        return new ChargerResponse(
                101L, 20L, "Vikram Host", "Indiranagar Wallbox", "Covered parking",
                LocationType.HOME, "12th Main", "Bengaluru", "Karnataka", "560038", "India",
                new BigDecimal("12.971600"), new BigDecimal("77.594600"),
                PowerType.AC, new BigDecimal("7.40"), new BigDecimal("18.50"),
                availability, verification, null, null, null,
                List.of(ConnectorType.TYPE_2),
                Instant.parse("2026-11-12T10:00:00Z"),
                Instant.parse("2026-11-12T10:00:00Z")
        );
    }
}

