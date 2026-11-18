package com.example.chargeconnect.session.controller;

import com.example.chargeconnect.exception.GlobalExceptionHandler;
import com.example.chargeconnect.session.dto.SessionResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HostSessionControllerTest {

    @Mock
    private ChargingSessionService sessionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Authentication hostAuth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HostSessionController(sessionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        hostAuth = new UsernamePasswordAuthenticationToken("host@test.com", null);
    }

    @Test
    void emergencyStopReturns200() throws Exception {
        SessionResponse response = new SessionResponse(
                100L, "SES-ABC123XYZ", 10L, 1L, 2L, "Fast Hub", 3L,
                SessionStatus.EMERGENCY_STOPPED, Instant.now().minusSeconds(900), Instant.now(),
                BigDecimal.ZERO, null, new BigDecimal("12.50"),
                new BigDecimal("15.00"), new BigDecimal("187.50"), "Host emergency cutoff", Instant.now(), Instant.now()
        );

        when(sessionService.emergencyStopByHost(eq("host@test.com"), eq(2L), eq(100L), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/host/chargers/2/sessions/100/emergency-stop")
                        .principal(hostAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StopSessionRequest("Host emergency cutoff"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EMERGENCY_STOPPED"))
                .andExpect(jsonPath("$.stopReason").value("Host emergency cutoff"));
    }

    @Test
    void getChargerSessionsReturns200() throws Exception {
        SessionResponse response = new SessionResponse(
                100L, "SES-ABC123XYZ", 10L, 1L, 2L, "Fast Hub", 3L,
                SessionStatus.COMPLETED, Instant.now(), Instant.now(),
                BigDecimal.ZERO, null, new BigDecimal("25.00"),
                new BigDecimal("15.00"), new BigDecimal("375.00"), "Done", Instant.now(), Instant.now()
        );

        when(sessionService.getHostChargerSessions("host@test.com", 2L))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/host/chargers/2/sessions")
                        .principal(hostAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100L))
                .andExpect(jsonPath("$[0].chargerId").value(2L));
    }
}
