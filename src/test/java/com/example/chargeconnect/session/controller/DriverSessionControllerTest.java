package com.example.chargeconnect.session.controller;

import com.example.chargeconnect.exception.GlobalExceptionHandler;
import com.example.chargeconnect.session.dto.LiveTelemetryResponse;
import com.example.chargeconnect.session.dto.SessionResponse;
import com.example.chargeconnect.session.dto.StartSessionRequest;
import com.example.chargeconnect.session.dto.StopSessionRequest;
import com.example.chargeconnect.session.model.SessionStatus;
import com.example.chargeconnect.session.service.ChargingSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DriverSessionControllerTest {

    @Mock
    private ChargingSessionService sessionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Authentication driverAuth;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new DriverSessionController(sessionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
        driverAuth = new UsernamePasswordAuthenticationToken("driver@test.com", null);
    }

    @Test
    void startSessionReturns201() throws Exception {
        SessionResponse response = new SessionResponse(
                100L, "SES-ABC123XYZ", 10L, 1L, 2L, "Fast Hub", 3L,
                SessionStatus.ACTIVE, Instant.now(), null,
                BigDecimal.ZERO, null, BigDecimal.ZERO,
                new BigDecimal("15.00"), null, null, Instant.now(), Instant.now()
        );

        when(sessionService.startSession(eq("driver@test.com"), any(StartSessionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/driver/sessions/start")
                        .principal(driverAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartSessionRequest(10L, "CHG-QR-2"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.sessionReference").value("SES-ABC123XYZ"));
    }

    @Test
    void stopSessionReturns200() throws Exception {
        SessionResponse response = new SessionResponse(
                100L, "SES-ABC123XYZ", 10L, 1L, 2L, "Fast Hub", 3L,
                SessionStatus.COMPLETED, Instant.now().minusSeconds(1800), Instant.now(),
                BigDecimal.ZERO, null, new BigDecimal("25.00"),
                new BigDecimal("15.00"), new BigDecimal("375.00"), "Battery full", Instant.now(), Instant.now()
        );

        when(sessionService.stopSessionByDriver(eq("driver@test.com"), eq(100L), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/driver/sessions/100/stop")
                        .principal(driverAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StopSessionRequest("Battery full"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.energyDeliveredKwh").value(25.00))
                .andExpect(jsonPath("$.totalCost").value(375.00));
    }

    @Test
    void getLiveTelemetryReturns200() throws Exception {
        LiveTelemetryResponse response = new LiveTelemetryResponse(
                100L, "SES-ABC123XYZ", SessionStatus.ACTIVE, Instant.now(),
                1800L, 30L, new BigDecimal("50.00"), new BigDecimal("25.00"),
                new BigDecimal("15.00"), new BigDecimal("375.00"), new BigDecimal("40.50"), 61.73
        );

        when(sessionService.getLiveTelemetry("driver@test.com", 100L))
                .thenReturn(response);

        mockMvc.perform(get("/api/driver/sessions/100/telemetry")
                        .principal(driverAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(100L))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentPowerKw").value(50.00))
                .andExpect(jsonPath("$.estimatedBatteryPercent").value(61.73));
    }

    @Test
    void getMySessionsReturns200() throws Exception {
        SessionResponse response = new SessionResponse(
                100L, "SES-ABC123XYZ", 10L, 1L, 2L, "Fast Hub", 3L,
                SessionStatus.COMPLETED, Instant.now(), Instant.now(),
                BigDecimal.ZERO, null, new BigDecimal("25.00"),
                new BigDecimal("15.00"), new BigDecimal("375.00"), "Done", Instant.now(), Instant.now()
        );

        when(sessionService.getDriverSessions("driver@test.com"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/driver/sessions")
                        .principal(driverAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100L));
    }
}
