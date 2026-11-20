package com.example.chargeconnect.payout.dto;

import com.example.chargeconnect.payout.model.HostPayout;
import com.example.chargeconnect.payout.model.PayoutMethod;
import com.example.chargeconnect.payout.model.PayoutStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PayoutResponse(
        Long id,
        String payoutReference,
        Long hostId,
        String hostName,
        BigDecimal amount,
        String currency,
        PayoutMethod payoutMethod,
        String accountDetails,
        PayoutStatus status,
        String transactionReference,
        Instant processedAt,
        String failureReason,
        Instant createdAt
) {
    public static PayoutResponse from(HostPayout payout) {
        return new PayoutResponse(
                payout.getId(),
                payout.getPayoutReference(),
                payout.getHost().getId(),
                payout.getHost().getFullName(),
                payout.getAmount(),
                payout.getCurrency(),
                payout.getPayoutMethod(),
                payout.getAccountDetails(),
                payout.getStatus(),
                payout.getTransactionReference(),
                payout.getProcessedAt(),
                payout.getFailureReason(),
                payout.getCreatedAt()
        );
    }
}
