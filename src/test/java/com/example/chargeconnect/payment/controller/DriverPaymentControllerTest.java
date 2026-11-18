package com.example.chargeconnect.payment.controller;

import com.example.chargeconnect.exception.GlobalExceptionHandler;
import com.example.chargeconnect.payment.dto.CreatePaymentOrderRequest;
import com.example.chargeconnect.payment.dto.PaymentOrderResponse;
import com.example.chargeconnect.payment.dto.PaymentResponse;
import com.example.chargeconnect.payment.dto.VerifyPaymentRequest;
import com.example.chargeconnect.payment.model.PaymentStatus;
import com.example.chargeconnect.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
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
class DriverPaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Authentication driverAuth;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new DriverPaymentController(paymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
        driverAuth = new UsernamePasswordAuthenticationToken("driver@test.com", null);
    }

    @Test
    void createOrderReturns201() throws Exception {
        PaymentOrderResponse mockResponse = new PaymentOrderResponse(
                1L, 10L, "order_12345", new BigDecimal("900.00"), "INR", "rzp_test_key"
        );

        when(paymentService.createPaymentOrder(eq("driver@test.com"), any(CreatePaymentOrderRequest.class)))
                .thenReturn(mockResponse);

        String body = """
                {
                  "bookingId": 10
                }
                """;

        mockMvc.perform(post("/api/driver/payments/create-order")
                        .principal(driverAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.razorpayOrderId").value("order_12345"))
                .andExpect(jsonPath("$.amount").value(900.00));
    }

    @Test
    void verifyPaymentReturns200() throws Exception {
        PaymentResponse mockResponse = new PaymentResponse(
                1L, 10L, "ref-uuid", 5L, "order_12345", "pay_99999",
                new BigDecimal("900.00"), "INR", PaymentStatus.SUCCESS,
                null, Instant.now(), Instant.now()
        );

        when(paymentService.verifyPayment(eq("driver@test.com"), any(VerifyPaymentRequest.class)))
                .thenReturn(mockResponse);

        String body = """
                {
                  "bookingId": 10,
                  "razorpayOrderId": "order_12345",
                  "razorpayPaymentId": "pay_99999",
                  "razorpaySignature": "valid_signature"
                }
                """;

        mockMvc.perform(post("/api/driver/payments/verify")
                        .principal(driverAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void getPaymentByBookingReturns200() throws Exception {
        PaymentResponse mockResponse = new PaymentResponse(
                1L, 10L, "ref-uuid", 5L, "order_12345", "pay_99999",
                new BigDecimal("900.00"), "INR", PaymentStatus.SUCCESS,
                null, Instant.now(), Instant.now()
        );

        when(paymentService.getPaymentByBooking("driver@test.com", 10L)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/driver/payments/booking/10").principal(driverAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}
