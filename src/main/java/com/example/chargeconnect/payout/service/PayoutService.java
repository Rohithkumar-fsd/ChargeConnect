package com.example.chargeconnect.payout.service;

import com.example.chargeconnect.exception.ResourceNotFoundException;
import com.example.chargeconnect.payout.dto.*;
import com.example.chargeconnect.payout.model.HostPayout;
import com.example.chargeconnect.payout.model.PayoutStatus;
import com.example.chargeconnect.payout.repo.HostPayoutRepository;
import com.example.chargeconnect.session.repo.ChargingSessionRepository;
import com.example.chargeconnect.user.model.User;
import com.example.chargeconnect.user.repo.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PayoutService {

    private static final BigDecimal COMMISSION_RATE = BigDecimal.valueOf(0.10); // 10% platform fee

    private final HostPayoutRepository payoutRepository;
    private final ChargingSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public PayoutService(
            HostPayoutRepository payoutRepository,
            ChargingSessionRepository sessionRepository,
            UserRepository userRepository
    ) {
        this.payoutRepository = payoutRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public HostEarningsSummaryResponse getHostEarningsSummary(String hostEmail) {
        User host = findUserByEmail(hostEmail);
        return calculateEarnings(host);
    }

    @Transactional
    public PayoutResponse requestPayout(String hostEmail, RequestPayoutRequest request) {
        User host = findUserByEmail(hostEmail);
        HostEarningsSummaryResponse summary = calculateEarnings(host);

        if (request.amount().compareTo(summary.availableBalance()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient available balance. Available: ₹" + summary.availableBalance() +
                    ", Requested: ₹" + request.amount()
            );
        }

        HostPayout payout = new HostPayout(
                host,
                request.amount().setScale(2, RoundingMode.HALF_UP),
                request.payoutMethod(),
                request.accountDetails()
        );

        HostPayout saved = payoutRepository.save(payout);
        return PayoutResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PayoutResponse> getHostPayouts(String hostEmail) {
        User host = findUserByEmail(hostEmail);
        return payoutRepository.findAllByHostIdOrderByCreatedAtDesc(host.getId())
                .stream()
                .map(PayoutResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PayoutResponse> getAllPayouts(PayoutStatus status) {
        List<HostPayout> payouts = (status != null)
                ? payoutRepository.findAllByStatusOrderByCreatedAtDesc(status)
                : payoutRepository.findAllByOrderByCreatedAtDesc();

        return payouts.stream()
                .map(PayoutResponse::from)
                .toList();
    }

    @Transactional
    public PayoutResponse processPayout(Long payoutId, ProcessPayoutRequest request) {
        HostPayout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Host payout not found with ID: " + payoutId));

        if (payout.getStatus() != PayoutStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only PENDING payouts can be processed. Current status: " + payout.getStatus()
            );
        }

        payout.process(request.transactionReference());
        HostPayout saved = payoutRepository.save(payout);
        return PayoutResponse.from(saved);
    }

    @Transactional
    public PayoutResponse rejectPayout(Long payoutId, RejectPayoutRequest request) {
        HostPayout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Host payout not found with ID: " + payoutId));

        if (payout.getStatus() != PayoutStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only PENDING payouts can be rejected. Current status: " + payout.getStatus()
            );
        }

        payout.reject(request.reason());
        HostPayout saved = payoutRepository.save(payout);
        return PayoutResponse.from(saved);
    }

    private HostEarningsSummaryResponse calculateEarnings(User host) {
        BigDecimal gross = sessionRepository.sumTotalCostByHostId(host.getId());
        if (gross == null) gross = BigDecimal.ZERO;
        gross = gross.setScale(2, RoundingMode.HALF_UP);

        BigDecimal platformFee = gross.multiply(COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netEarnings = gross.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalWithdrawn = payoutRepository.sumProcessedPayoutsByHostId(host.getId());
        if (totalWithdrawn == null) totalWithdrawn = BigDecimal.ZERO;
        totalWithdrawn = totalWithdrawn.setScale(2, RoundingMode.HALF_UP);

        BigDecimal pendingPayouts = payoutRepository.sumPendingPayoutsByHostId(host.getId());
        if (pendingPayouts == null) pendingPayouts = BigDecimal.ZERO;
        pendingPayouts = pendingPayouts.setScale(2, RoundingMode.HALF_UP);

        BigDecimal availableBalance = netEarnings.subtract(totalWithdrawn).subtract(pendingPayouts);
        if (availableBalance.compareTo(BigDecimal.ZERO) < 0) {
            availableBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return new HostEarningsSummaryResponse(
                host.getId(),
                host.getFullName(),
                gross,
                platformFee,
                netEarnings,
                totalWithdrawn,
                pendingPayouts,
                availableBalance
        );
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
