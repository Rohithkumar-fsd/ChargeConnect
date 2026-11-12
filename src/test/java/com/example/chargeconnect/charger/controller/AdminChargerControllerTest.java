package com.example.chargeconnect.charger.controller;

import com.example.chargeconnect.charger.AvailabilityStatus;
import com.example.chargeconnect.charger.LocationType;
import com.example.chargeconnect.charger.PowerType;
import com.example.chargeconnect.charger.VerificationStatus;
import com.example.chargeconnect.charger.dto.ChargerResponse;
import com.example.chargeconnect.charger.service.ChargerService;
import com.example.chargeconnect.exception.GlobalExceptionHandler;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminChargerControllerTest {

    @Mock
    private ChargerService chargerService;

    private MockMvc mockMvc;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AdminChargerController(chargerService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        adminAuth = new UsernamePasswordAuthenticationToken("admin@example.com", null);
    }

    @Test
    void getChargersPassesOptionalStatusFilter() throws Exception {
        ChargerResponse response = new ChargerResponse(
                101L, 20L, "Vikram Host", "Indiranagar Wallbox", null,
                LocationType.HOME, "12th Main", "Bengaluru", "Karnataka", "560038", "India",
                new BigDecimal("12.971600"), new BigDecimal("77.594600"),
                PowerType.AC, new BigDecimal("7.40"), new BigDecimal("18.50"),
                AvailabilityStatus.AVAILABLE, VerificationStatus.PENDING, null, null, null,
                List.of(ConnectorType.TYPE_2),
                Instant.parse("2026-11-12T10:00:00Z"),
                Instant.parse("2026-11-12T10:00:00Z")
        );

        when(chargerService.getChargersForAdmin(VerificationStatus.PENDING)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/admin/chargers?status=PENDING").principal(adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].verificationStatus").value("PENDING"));
    }

    @Test
    void verifyChargerReturnsApproved() throws Exception {
        ChargerResponse approved = new ChargerResponse(
                101L, 20L, "Vikram Host", "Indiranagar Wallbox", null,
                LocationType.HOME, "12th Main", "Bengaluru", "Karnataka", "560038", "India",
                new BigDecimal("12.971600"), new BigDecimal("77.594600"),
                PowerType.AC, new BigDecimal("7.40"), new BigDecimal("18.50"),
                AvailabilityStatus.AVAILABLE, VerificationStatus.APPROVED, null, 1L,
                Instant.parse("2026-11-12T11:00:00Z"),
                List.of(ConnectorType.TYPE_2),
                Instant.parse("2026-11-12T10:00:00Z"),
                Instant.parse("2026-11-12T11:00:00Z")
        );

        when(chargerService.verifyCharger(eq("admin@example.com"), eq(101L), any())).thenReturn(approved);

        mockMvc.perform(patch("/api/admin/chargers/101/verification")
                        .principal(adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("APPROVED"))
                .andExpect(jsonPath("$.verifiedByUserId").value(1));
    }
}

