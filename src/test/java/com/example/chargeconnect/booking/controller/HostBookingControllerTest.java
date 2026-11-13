package com.example.chargeconnect.booking.controller;

import com.example.chargeconnect.booking.dto.BookingResponse;
import com.example.chargeconnect.booking.dto.HostBlockSlotRequest;
import com.example.chargeconnect.booking.model.BookingStatus;
import com.example.chargeconnect.booking.service.BookingService;
import com.example.chargeconnect.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
class HostBookingControllerTest {

    @Mock
    private BookingService bookingService;

    private MockMvc mockMvc;
    private Authentication hostAuth;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new HostBookingController(bookingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        hostAuth = new UsernamePasswordAuthenticationToken("host@test.com", null);
    }

    @Test
    void getChargerBookingsReturns200() throws Exception {
        Instant start = LocalDate.now().plusDays(1).atTime(10, 0).toInstant(ZoneOffset.UTC);
        Instant end = LocalDate.now().plusDays(1).atTime(11, 0).toInstant(ZoneOffset.UTC);

        BookingResponse mockResponse = new BookingResponse(
                1L, "ref-uuid", 5L, "Driver Dave", 10L, "Station A", 20L, "Tata Nexon",
                start, end, new BigDecimal("1.00"), new BigDecimal("15.00"),
                new BigDecimal("30.00"), new BigDecimal("450.00"),
                BookingStatus.CONFIRMED, null, Instant.now(), Instant.now()
        );

        when(bookingService.getHostChargerBookings("host@test.com", 10L)).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/host/chargers/10/bookings").principal(hostAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void blockSlotReturns201() throws Exception {
        Instant start = LocalDate.now().plusDays(1).atTime(9, 0).toInstant(ZoneOffset.UTC);
        Instant end = LocalDate.now().plusDays(1).atTime(10, 0).toInstant(ZoneOffset.UTC);

        BookingResponse mockResponse = new BookingResponse(
                2L, "ref-uuid-2", 2L, "Host Helen", 10L, "Station A", null, null,
                start, end, new BigDecimal("1.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BookingStatus.BLOCKED_BY_HOST, "Personal use", Instant.now(), Instant.now()
        );

        when(bookingService.blockSlotForHost(eq("host@test.com"), eq(10L), any(HostBlockSlotRequest.class)))
                .thenReturn(mockResponse);

        String body = String.format("""
                {
                  "startTime": "%s",
                  "endTime": "%s",
                  "reason": "Personal use"
                }
                """, start, end);

        mockMvc.perform(post("/api/host/chargers/10/block-slots")
                        .principal(hostAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.status").value("BLOCKED_BY_HOST"));
    }
}
