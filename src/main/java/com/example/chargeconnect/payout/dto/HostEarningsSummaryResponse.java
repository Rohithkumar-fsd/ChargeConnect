package com.example.chargeconnect.payout.dto;

import java.math.BigDecimal;

public record HostEarningsSummaryResponse(
        Long hostId,
        String hostName,
        BigDecimal grossEarnings,
        BigDecimal platformCommission,
        BigDecimal netEarnings,
        BigDecimal totalWithdrawn,
        BigDecimal pendingPayouts,
        BigDecimal availableBalance
) {}
