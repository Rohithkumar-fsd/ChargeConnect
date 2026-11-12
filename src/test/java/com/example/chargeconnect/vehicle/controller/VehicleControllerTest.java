package com.example.chargeconnect.vehicle.controller;

import com.example.chargeconnect.exception.DuplicateResourceException;
import com.example.chargeconnect.exception.GlobalExceptionHandler;
import com.example.chargeconnect.exception.ResourceNotFoundException;
import com.example.chargeconnect.vehicle.ConnectorType;
import com.example.chargeconnect.vehicle.dto.VehicleResponse;
import com.example.chargeconnect.vehicle.service.VehicleService;
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
class VehicleControllerTest {

    @Mock
    private VehicleService vehicleService;

    private MockMvc mockMvc;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new VehicleController(vehicleService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        auth = new UsernamePasswordAuthenticationToken("asha@example.com", null);
    }

    @Test
    void createVehicleReturnsCreated() throws Exception {
        VehicleResponse response = new VehicleResponse(
                1L,
                "Tata",
                "Nexon EV",
                "KA01AB1234",
                new BigDecimal("40.50"),
                new BigDecimal("7.20"),
                new BigDecimal("50.00"),
                List.of(ConnectorType.CCS2, ConnectorType.TYPE_2),
                Instant.parse("2026-11-12T10:00:00Z"),
                Instant.parse("2026-11-12T10:00:00Z")
        );

        when(vehicleService.createVehicle(eq("asha@example.com"), any())).thenReturn(response);

        String body = """
                {
                  "manufacturer": "Tata",
                  "model": "Nexon EV",
                  "registrationNumber": "KA01AB1234",
                  "batteryCapacityKwh": 40.50,
                  "maxAcPowerKw": 7.20,
                  "maxDcPowerKw": 50.00,
                  "connectorTypes": ["CCS2", "TYPE_2"]
                }
                """;

        mockMvc.perform(post("/api/driver/vehicles")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.manufacturer").value("Tata"))
                .andExpect(jsonPath("$.connectorTypes[0]").value("CCS2"));
    }

    @Test
    void createVehicleReturnsBadRequestWhenInvalid() throws Exception {
        String invalidBody = """
                {
                  "manufacturer": "",
                  "model": "Nexon EV",
                  "batteryCapacityKwh": -5,
                  "connectorTypes": []
                }
                """;

        mockMvc.perform(post("/api/driver/vehicles")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.manufacturer").exists())
                .andExpect(jsonPath("$.fieldErrors.batteryCapacityKwh").exists())
                .andExpect(jsonPath("$.fieldErrors.connectorTypes").exists());
    }

    @Test
    void createVehicleReturnsConflictOnDuplicateRegistration() throws Exception {
        when(vehicleService.createVehicle(eq("asha@example.com"), any()))
                .thenThrow(new DuplicateResourceException("A vehicle with this registration number already exists"));

        String body = """
                {
                  "manufacturer": "Tata",
                  "model": "Nexon EV",
                  "registrationNumber": "KA01AB1234",
                  "batteryCapacityKwh": 40.50,
                  "connectorTypes": ["CCS2"]
                }
                """;

        mockMvc.perform(post("/api/driver/vehicles")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getMyVehicleReturnsNotFoundWhenMissing() throws Exception {
        when(vehicleService.getMyVehicle("asha@example.com", 99L))
                .thenThrow(new ResourceNotFoundException("Vehicle not found"));

        mockMvc.perform(get("/api/driver/vehicles/99").principal(auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteVehicleReturnsNoContent() throws Exception {
        doNothing().when(vehicleService).deleteVehicle("asha@example.com", 1L);

        mockMvc.perform(delete("/api/driver/vehicles/1").principal(auth))
                .andExpect(status().isNoContent());
    }
}

