package com.example.chargeconnect.payout.service;

import com.example.chargeconnect.exception.ResourceNotFoundException;
import com.example.chargeconnect.payout.dto.*;
import com.example.chargeconnect.payout.model.HostPayout;
import com.example.chargeconnect.payout.model.PayoutMethod;
import com.example.chargeconnect.payout.model.PayoutStatus;
import com.example.chargeconnect.payout.repo.HostPayoutRepository;
import com.example.chargeconnect.session.repo.ChargingSessionRepository;
import com.example.chargeconnect.user.model.Role;
import com.example.chargeconnect.user.model.User;
import com.example.chargeconnect.user.repo.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock private HostPayoutRepository payoutRepository;
    @Mock private ChargingSessionRepository sessionRepository;
    @Mock private UserRepository userRepository;

    private PayoutService payoutService;
    private User host;

    @BeforeEach
    void setUp() {
        payoutService = new PayoutService(payoutRepository, sessionRepository, userRepository);

        host = new User("Kiran Host", "host@test.com", "9876543210", "hashedPassword");
        host.addRole(Role.HOST);
        ReflectionTestUtils.setField(host, "id", 10L);
    }

    @Test
    void getHostEarningsSummary_Success() {
        when(userRepository.findByEmail("host@test.com")).thenReturn(Optional.of(host));
        when(sessionRepository.sumTotalCostByHostId(10L)).thenReturn(BigDecimal.valueOf(1000.00));
        when(payoutRepository.sumProcessedPayoutsByHostId(10L)).thenReturn(BigDecimal.valueOf(300.00));
        when(payoutRepository.sumPendingPayoutsByHostId(10L)).thenReturn(BigDecimal.valueOf(100.00));

        HostEarningsSummaryResponse summary = payoutService.getHostEarningsSummary("host@test.com");

        assertThat(summary.hostId()).isEqualTo(10L);
        assertThat(summary.hostName()).isEqualTo("Kiran Host");
        assertThat(summary.grossEarnings()).isEqualByComparingTo("1000.00");
        assertThat(summary.platformCommission()).isEqualByComparingTo("100.00"); // 10%
        assertThat(summary.netEarnings()).isEqualByComparingTo("900.00"); // 90%
        assertThat(summary.totalWithdrawn()).isEqualByComparingTo("300.00");
        assertThat(summary.pendingPayouts()).isEqualByComparingTo("100.00");
        assertThat(summary.availableBalance()).isEqualByComparingTo("500.00"); // 900 - 300 - 100
    }

    @Test
    void requestPayout_Success() {
        when(userRepository.findByEmail("host@test.com")).thenReturn(Optional.of(host));
        when(sessionRepository.sumTotalCostByHostId(10L)).thenReturn(BigDecimal.valueOf(1000.00));
        when(payoutRepository.sumProcessedPayoutsByHostId(10L)).thenReturn(BigDecimal.valueOf(300.00));
        when(payoutRepository.sumPendingPayoutsByHostId(10L)).thenReturn(BigDecimal.valueOf(100.00)); // available = 500.00

        when(payoutRepository.save(any(HostPayout.class))).thenAnswer(invocation -> {
            HostPayout p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 1L);
            return p;
        });

        RequestPayoutRequest request = new RequestPayoutRequest(
                BigDecimal.valueOf(250.00),
                PayoutMethod.UPI,
                "kiran@okaxis"
        );

        PayoutResponse response = payoutService.requestPayout("host@test.com", request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.amount()).isEqualByComparingTo("250.00");
        assertThat(response.status()).isEqualTo(PayoutStatus.PENDING);
        assertThat(response.payoutMethod()).isEqualTo(PayoutMethod.UPI);
        assertThat(response.accountDetails()).isEqualTo("kiran@okaxis");
        verify(payoutRepository).save(any(HostPayout.class));
    }

    @Test
    void requestPayout_InsufficientBalance_ThrowsException() {
        when(userRepository.findByEmail("host@test.com")).thenReturn(Optional.of(host));
        when(sessionRepository.sumTotalCostByHostId(10L)).thenReturn(BigDecimal.valueOf(100.00)); // net = 90.00
        when(payoutRepository.sumProcessedPayoutsByHostId(10L)).thenReturn(BigDecimal.ZERO);
        when(payoutRepository.sumPendingPayoutsByHostId(10L)).thenReturn(BigDecimal.ZERO); // available = 90.00

        RequestPayoutRequest request = new RequestPayoutRequest(
                BigDecimal.valueOf(100.00), // requesting 100 > 90
                PayoutMethod.UPI,
                "kiran@okaxis"
        );

        assertThatThrownBy(() -> payoutService.requestPayout("host@test.com", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Insufficient available balance");

        verify(payoutRepository, never()).save(any(HostPayout.class));
    }

    @Test
    void requestPayout_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        RequestPayoutRequest request = new RequestPayoutRequest(
                BigDecimal.valueOf(100.00),
                PayoutMethod.UPI,
                "kiran@okaxis"
        );

        assertThatThrownBy(() -> payoutService.requestPayout("unknown@test.com", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void processPayout_Success() {
        HostPayout payout = new HostPayout(host, BigDecimal.valueOf(200.00), PayoutMethod.UPI, "kiran@okaxis");
        ReflectionTestUtils.setField(payout, "id", 5L);

        when(payoutRepository.findById(5L)).thenReturn(Optional.of(payout));
        when(payoutRepository.save(any(HostPayout.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessPayoutRequest request = new ProcessPayoutRequest("UTR-202611200001");
        PayoutResponse response = payoutService.processPayout(5L, request);

        assertThat(response.status()).isEqualTo(PayoutStatus.PROCESSED);
        assertThat(response.transactionReference()).isEqualTo("UTR-202611200001");
        assertThat(response.processedAt()).isNotNull();
    }

    @Test
    void processPayout_AlreadyProcessed_ThrowsException() {
        HostPayout payout = new HostPayout(host, BigDecimal.valueOf(200.00), PayoutMethod.UPI, "kiran@okaxis");
        payout.process("UTR-EXISTING");
        ReflectionTestUtils.setField(payout, "id", 5L);

        when(payoutRepository.findById(5L)).thenReturn(Optional.of(payout));

        ProcessPayoutRequest request = new ProcessPayoutRequest("UTR-NEW");

        assertThatThrownBy(() -> payoutService.processPayout(5L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only PENDING payouts can be processed");
    }

    @Test
    void rejectPayout_Success() {
        HostPayout payout = new HostPayout(host, BigDecimal.valueOf(200.00), PayoutMethod.UPI, "invalid@upi");
        ReflectionTestUtils.setField(payout, "id", 7L);

        when(payoutRepository.findById(7L)).thenReturn(Optional.of(payout));
        when(payoutRepository.save(any(HostPayout.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RejectPayoutRequest request = new RejectPayoutRequest("Invalid UPI VPA address");
        PayoutResponse response = payoutService.rejectPayout(7L, request);

        assertThat(response.status()).isEqualTo(PayoutStatus.REJECTED);
        assertThat(response.failureReason()).isEqualTo("Invalid UPI VPA address");
    }

    @Test
    void rejectPayout_AlreadyProcessed_ThrowsException() {
        HostPayout payout = new HostPayout(host, BigDecimal.valueOf(200.00), PayoutMethod.UPI, "kiran@okaxis");
        payout.process("UTR-DONE");
        ReflectionTestUtils.setField(payout, "id", 7L);

        when(payoutRepository.findById(7L)).thenReturn(Optional.of(payout));

        RejectPayoutRequest request = new RejectPayoutRequest("Reason");

        assertThatThrownBy(() -> payoutService.rejectPayout(7L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only PENDING payouts can be rejected");
    }

    @Test
    void getHostPayouts_Success() {
        HostPayout payout = new HostPayout(host, BigDecimal.valueOf(150.00), PayoutMethod.BANK_TRANSFER, "ACC:1234");
        ReflectionTestUtils.setField(payout, "id", 1L);

        when(userRepository.findByEmail("host@test.com")).thenReturn(Optional.of(host));
        when(payoutRepository.findAllByHostIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(payout));

        List<PayoutResponse> payouts = payoutService.getHostPayouts("host@test.com");

        assertThat(payouts).hasSize(1);
        assertThat(payouts.get(0).amount()).isEqualByComparingTo("150.00");
    }

    @Test
    void getAllPayouts_FilteredByStatus_Success() {
        HostPayout payout = new HostPayout(host, BigDecimal.valueOf(150.00), PayoutMethod.UPI, "kiran@okaxis");
        ReflectionTestUtils.setField(payout, "id", 1L);

        when(payoutRepository.findAllByStatusOrderByCreatedAtDesc(PayoutStatus.PENDING)).thenReturn(List.of(payout));

        List<PayoutResponse> payouts = payoutService.getAllPayouts(PayoutStatus.PENDING);

        assertThat(payouts).hasSize(1);
        verify(payoutRepository).findAllByStatusOrderByCreatedAtDesc(PayoutStatus.PENDING);
    }
}
