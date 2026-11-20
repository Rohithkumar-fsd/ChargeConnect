package com.example.chargeconnect.analytics.controller;

import com.example.chargeconnect.analytics.dto.AdminAnalyticsResponse;
import com.example.chargeconnect.analytics.service.AdminAnalyticsService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsControllerTest {

    @Mock
    private AdminAnalyticsService analyticsService;

    private MockMvc mockMvc;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminAnalyticsController(analyticsService))
                .build();

        adminAuth = new UsernamePasswordAuthenticationToken("admin@chargeconnect.com", null);
    }

    @Test
    void getAnalytics_Returns200() throws Exception {
        AdminAnalyticsResponse response = new AdminAnalyticsResponse(
                20L, 15L, 4L,
                10L, 8L, 2L,
                50L, 5L, 45L,
                BigDecimal.valueOf(500.00),
                BigDecimal.valueOf(7500.00),
                BigDecimal.valueOf(750.00),
                BigDecimal.valueOf(3000.00),
                BigDecimal.valueOf(500.00)
        );

        when(analyticsService.getPlatformAnalytics()).thenReturn(response);

        mockMvc.perform(get("/api/admin/analytics")
                        .principal(adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(20))
                .andExpect(jsonPath("$.totalDrivers").value(15))
                .andExpect(jsonPath("$.totalHosts").value(4))
                .andExpect(jsonPath("$.totalChargers").value(10))
                .andExpect(jsonPath("$.approvedChargers").value(8))
                .andExpect(jsonPath("$.totalSessions").value(50))
                .andExpect(jsonPath("$.totalEnergyDeliveredKwh").value(500.00))
                .andExpect(jsonPath("$.totalGrossVolume").value(7500.00))
                .andExpect(jsonPath("$.platformCommissionEarned").value(750.00));
    }
}
