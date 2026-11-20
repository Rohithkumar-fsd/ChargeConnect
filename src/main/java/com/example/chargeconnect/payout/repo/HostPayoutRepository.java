package com.example.chargeconnect.payout.repo;

import com.example.chargeconnect.payout.model.HostPayout;
import com.example.chargeconnect.payout.model.PayoutStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HostPayoutRepository extends JpaRepository<HostPayout, Long> {

    List<HostPayout> findAllByHostIdOrderByCreatedAtDesc(Long hostId);

    List<HostPayout> findAllByOrderByCreatedAtDesc();

    List<HostPayout> findAllByStatusOrderByCreatedAtDesc(PayoutStatus status);

    Optional<HostPayout> findByIdAndHostId(Long id, Long hostId);

    Optional<HostPayout> findByPayoutReference(String payoutReference);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM HostPayout p WHERE p.host.id = :hostId AND p.status = com.example.chargeconnect.payout.model.PayoutStatus.PROCESSED")
    BigDecimal sumProcessedPayoutsByHostId(@Param("hostId") Long hostId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM HostPayout p WHERE p.host.id = :hostId AND p.status = com.example.chargeconnect.payout.model.PayoutStatus.PENDING")
    BigDecimal sumPendingPayoutsByHostId(@Param("hostId") Long hostId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM HostPayout p WHERE p.status = com.example.chargeconnect.payout.model.PayoutStatus.PROCESSED")
    BigDecimal sumTotalProcessedPayouts();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM HostPayout p WHERE p.status = com.example.chargeconnect.payout.model.PayoutStatus.PENDING")
    BigDecimal sumTotalPendingPayouts();
}
