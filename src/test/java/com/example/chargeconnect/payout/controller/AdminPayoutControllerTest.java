package com.example.chargeconnect.payout.controller;

import com.example.chargeconnect.exception.GlobalExceptionHandler;
import com.example.chargeconnect.payout.dto.PayoutResponse;
import com.example.chargeconnect.payout.dto.ProcessPayoutRequest;
import com.example.chargeconnect.payout.dto.RejectPayoutRequest;
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
class AdminPayoutControllerTest {

    @Mock
    private PayoutService payoutService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AdminPayoutController(payoutService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
        adminAuth = new UsernamePasswordAuthenticationToken("admin@chargeconnect.com", null);
    }

    @Test
    void getAllPayouts_Returns200() throws Exception {
        PayoutResponse response = new PayoutResponse(
                1L, "PAY-REF123", 10L, "Kiran Host",
                BigDecimal.valueOf(200.00), "INR", PayoutMethod.UPI,
                "kiran@okaxis", PayoutStatus.PENDING, null, null, null, Instant.now()
        );

        when(payoutService.getAllPayouts(null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/admin/payouts")
                        .principal(adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void processPayout_Returns200() throws Exception {
        ProcessPayoutRequest request = new ProcessPayoutRequest("UTR-202611200001");
        PayoutResponse response = new PayoutResponse(
                1L, "PAY-REF123", 10L, "Kiran Host",
                BigDecimal.valueOf(200.00), "INR", PayoutMethod.UPI,
                "kiran@okaxis", PayoutStatus.PROCESSED, "UTR-202611200001", Instant.now(), null, Instant.now()
        );

        when(payoutService.processPayout(eq(1L), any(ProcessPayoutRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/payouts/1/process")
                        .principal(adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.transactionReference").value("UTR-202611200001"));
    }

    @Test
    void processPayout_BlankTransactionRef_Returns400() throws Exception {
        ProcessPayoutRequest request = new ProcessPayoutRequest(""); // Blank

        mockMvc.perform(post("/api/admin/payouts/1/process")
                        .principal(adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectPayout_Returns200() throws Exception {
        RejectPayoutRequest request = new RejectPayoutRequest("Account inactive");
        PayoutResponse response = new PayoutResponse(
                1L, "PAY-REF123", 10L, "Kiran Host",
                BigDecimal.valueOf(200.00), "INR", PayoutMethod.UPI,
                "kiran@okaxis", PayoutStatus.REJECTED, null, Instant.now(), "Account inactive", Instant.now()
        );

        when(payoutService.rejectPayout(eq(1L), any(RejectPayoutRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/payouts/1/reject")
                        .principal(adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.failureReason").value("Account inactive"));
    }
}
