package com.example.chargeconnect.payout.controller;

import com.example.chargeconnect.exception.GlobalExceptionHandler;
import com.example.chargeconnect.payout.dto.HostEarningsSummaryResponse;
import com.example.chargeconnect.payout.dto.PayoutResponse;
import com.example.chargeconnect.payout.dto.RequestPayoutRequest;
import com.example.chargeconnect.payout.model.PayoutMethod;
import com.example.chargeconnect.payout.model.PayoutStatus;
import com.example.chargeconnect.payout.service.PayoutService;
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
class HostPayoutControllerTest {

    @Mock
    private PayoutService payoutService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Authentication hostAuth;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new HostPayoutController(payoutService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
        hostAuth = new UsernamePasswordAuthenticationToken("host@test.com", null);
    }

    @Test
    void getEarningsSummary_Returns200() throws Exception {
        HostEarningsSummaryResponse summary = new HostEarningsSummaryResponse(
                10L, "Kiran Host",
                BigDecimal.valueOf(1000.00), BigDecimal.valueOf(100.00),
                BigDecimal.valueOf(900.00), BigDecimal.valueOf(300.00),
                BigDecimal.valueOf(100.00), BigDecimal.valueOf(500.00)
        );

        when(payoutService.getHostEarningsSummary("host@test.com")).thenReturn(summary);

        mockMvc.perform(get("/api/host/payouts/earnings")
                        .principal(hostAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostId").value(10))
                .andExpect(jsonPath("$.grossEarnings").value(1000.00))
                .andExpect(jsonPath("$.platformCommission").value(100.00))
                .andExpect(jsonPath("$.netEarnings").value(900.00))
                .andExpect(jsonPath("$.availableBalance").value(500.00));
    }

    @Test
    void requestPayout_Returns201() throws Exception {
        RequestPayoutRequest request = new RequestPayoutRequest(
                BigDecimal.valueOf(200.00),
                PayoutMethod.UPI,
                "kiran@okaxis"
        );

        PayoutResponse response = new PayoutResponse(
                1L, "PAY-REF123", 10L, "Kiran Host",
                BigDecimal.valueOf(200.00), "INR", PayoutMethod.UPI,
                "kiran@okaxis", PayoutStatus.PENDING, null, null, null, Instant.now()
        );

        when(payoutService.requestPayout(eq("host@test.com"), any(RequestPayoutRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/host/payouts/request")
                        .principal(hostAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(200.00))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.payoutMethod").value("UPI"));
    }

    @Test
    void requestPayout_BelowMinimumAmount_Returns400() throws Exception {
        RequestPayoutRequest request = new RequestPayoutRequest(
                BigDecimal.valueOf(20.00), // Below 50.00 minimum
                PayoutMethod.UPI,
                "kiran@okaxis"
        );

        mockMvc.perform(post("/api/host/payouts/request")
                        .principal(hostAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPayouts_Returns200() throws Exception {
        PayoutResponse response = new PayoutResponse(
                1L, "PAY-REF123", 10L, "Kiran Host",
                BigDecimal.valueOf(200.00), "INR", PayoutMethod.UPI,
                "kiran@okaxis", PayoutStatus.PENDING, null, null, null, Instant.now()
        );

        when(payoutService.getHostPayouts("host@test.com")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/host/payouts")
                        .principal(hostAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].amount").value(200.00));
    }
}
