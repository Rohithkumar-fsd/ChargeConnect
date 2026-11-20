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
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAnalyticsService {

    private static final BigDecimal COMMISSION_RATE = BigDecimal.valueOf(0.10);

    private final UserRepository userRepository;
    private final ChargerRepository chargerRepository;
    private final ChargingSessionRepository sessionRepository;
    private final HostPayoutRepository payoutRepository;

    public AdminAnalyticsService(
            UserRepository userRepository,
            ChargerRepository chargerRepository,
            ChargingSessionRepository sessionRepository,
            HostPayoutRepository payoutRepository
    ) {
        this.userRepository = userRepository;
        this.chargerRepository = chargerRepository;
        this.sessionRepository = sessionRepository;
        this.payoutRepository = payoutRepository;
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getPlatformAnalytics() {
        long totalUsers = userRepository.count();
        long totalDrivers = userRepository.countByRole(Role.DRIVER);
        long totalHosts = userRepository.countByRole(Role.HOST);

        long totalChargers = chargerRepository.count();
        long approvedChargers = chargerRepository.countByVerificationStatus(VerificationStatus.APPROVED);
        long pendingChargers = chargerRepository.countByVerificationStatus(VerificationStatus.PENDING);

        long totalSessions = sessionRepository.count();
        long activeSessions = sessionRepository.countByStatus(SessionStatus.ACTIVE);
        long completedSessions = sessionRepository.countByStatusIn(
                List.of(SessionStatus.COMPLETED, SessionStatus.EMERGENCY_STOPPED)
        );

        BigDecimal totalEnergyDeliveredKwh = sessionRepository.sumTotalEnergyDeliveredKwh();
        if (totalEnergyDeliveredKwh == null) totalEnergyDeliveredKwh = BigDecimal.ZERO;
        totalEnergyDeliveredKwh = totalEnergyDeliveredKwh.setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalGrossVolume = sessionRepository.sumTotalGrossVolume();
        if (totalGrossVolume == null) totalGrossVolume = BigDecimal.ZERO;
        totalGrossVolume = totalGrossVolume.setScale(2, RoundingMode.HALF_UP);

        BigDecimal platformCommissionEarned = totalGrossVolume.multiply(COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalHostPayoutsPaid = payoutRepository.sumTotalProcessedPayouts();
        if (totalHostPayoutsPaid == null) totalHostPayoutsPaid = BigDecimal.ZERO;
        totalHostPayoutsPaid = totalHostPayoutsPaid.setScale(2, RoundingMode.HALF_UP);

        BigDecimal pendingHostPayouts = payoutRepository.sumTotalPendingPayouts();
        if (pendingHostPayouts == null) pendingHostPayouts = BigDecimal.ZERO;
        pendingHostPayouts = pendingHostPayouts.setScale(2, RoundingMode.HALF_UP);

        return new AdminAnalyticsResponse(
                totalUsers,
                totalDrivers,
                totalHosts,
                totalChargers,
                approvedChargers,
                pendingChargers,
                totalSessions,
                activeSessions,
                completedSessions,
                totalEnergyDeliveredKwh,
                totalGrossVolume,
                platformCommissionEarned,
                totalHostPayoutsPaid,
                pendingHostPayouts
        );
    }
}
