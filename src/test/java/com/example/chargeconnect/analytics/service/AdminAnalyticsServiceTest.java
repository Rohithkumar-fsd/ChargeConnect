package com.example.chargeconnect.analytics.service;

import com.example.chargeconnect.analytics.dto.AdminAnalyticsResponse;
import com.example.chargeconnect.charger.dto.VerificationStatus;
import com.example.chargeconnect.charger.repo.ChargerRepository;
import com.example.chargeconnect.payout.repo.HostPayoutRepository;
import com.example.chargeconnect.session.model.SessionStatus;
import com.example.chargeconnect.session.repo.ChargingSessionRepository;
import com.example.chargeconnect.user.model.Role;
import com.example.chargeconnect.user.repo.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ChargerRepository chargerRepository;
    @Mock private ChargingSessionRepository sessionRepository;
    @Mock private HostPayoutRepository payoutRepository;

    private AdminAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AdminAnalyticsService(
                userRepository,
                chargerRepository,
                sessionRepository,
                payoutRepository
        );
    }

    @Test
    void getPlatformAnalytics_Success() {
        when(userRepository.count()).thenReturn(20L);
        when(userRepository.countByRole(Role.DRIVER)).thenReturn(15L);
        when(userRepository.countByRole(Role.HOST)).thenReturn(4L);

        when(chargerRepository.count()).thenReturn(10L);
        when(chargerRepository.countByVerificationStatus(VerificationStatus.APPROVED)).thenReturn(8L);
        when(chargerRepository.countByVerificationStatus(VerificationStatus.PENDING)).thenReturn(2L);

        when(sessionRepository.count()).thenReturn(50L);
        when(sessionRepository.countByStatus(SessionStatus.ACTIVE)).thenReturn(5L);
        when(sessionRepository.countByStatusIn(List.of(SessionStatus.COMPLETED, SessionStatus.EMERGENCY_STOPPED))).thenReturn(45L);

        when(sessionRepository.sumTotalEnergyDeliveredKwh()).thenReturn(BigDecimal.valueOf(500.00));
        when(sessionRepository.sumTotalGrossVolume()).thenReturn(BigDecimal.valueOf(7500.00));

        when(payoutRepository.sumTotalProcessedPayouts()).thenReturn(BigDecimal.valueOf(3000.00));
        when(payoutRepository.sumTotalPendingPayouts()).thenReturn(BigDecimal.valueOf(500.00));

        AdminAnalyticsResponse response = analyticsService.getPlatformAnalytics();

        assertThat(response.totalUsers()).isEqualTo(20L);
        assertThat(response.totalDrivers()).isEqualTo(15L);
        assertThat(response.totalHosts()).isEqualTo(4L);

        assertThat(response.totalChargers()).isEqualTo(10L);
        assertThat(response.approvedChargers()).isEqualTo(8L);
        assertThat(response.pendingChargers()).isEqualTo(2L);

        assertThat(response.totalSessions()).isEqualTo(50L);
        assertThat(response.activeSessions()).isEqualTo(5L);
        assertThat(response.completedSessions()).isEqualTo(45L);

        assertThat(response.totalEnergyDeliveredKwh()).isEqualByComparingTo("500.00");
        assertThat(response.totalGrossVolume()).isEqualByComparingTo("7500.00");
        assertThat(response.platformCommissionEarned()).isEqualByComparingTo("750.00"); // 10% of 7500.00
        assertThat(response.totalHostPayoutsPaid()).isEqualByComparingTo("3000.00");
        assertThat(response.pendingHostPayouts()).isEqualByComparingTo("500.00");
    }
}
