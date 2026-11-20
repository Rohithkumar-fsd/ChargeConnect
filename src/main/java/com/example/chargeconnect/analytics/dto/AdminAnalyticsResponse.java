package com.example.chargeconnect.analytics.dto;

import java.math.BigDecimal;

public record AdminAnalyticsResponse(
        long totalUsers,
        long totalDrivers,
        long totalHosts,
        long totalChargers,
        long approvedChargers,
        long pendingChargers,
        long totalSessions,
        long activeSessions,
        long completedSessions,
        BigDecimal totalEnergyDeliveredKwh,
        BigDecimal totalGrossVolume,
        BigDecimal platformCommissionEarned,
        BigDecimal totalHostPayoutsPaid,
        BigDecimal pendingHostPayouts
) {}
