package com.example.chargeconnect.session.repo;

import com.example.chargeconnect.session.model.ChargingSession;
import com.example.chargeconnect.session.model.SessionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargingSessionRepository extends JpaRepository<ChargingSession, Long> {

    Optional<ChargingSession> findByBookingId(Long bookingId);

    Optional<ChargingSession> findByIdAndDriverId(Long id, Long driverId);

    Optional<ChargingSession> findByIdAndChargerHostId(Long id, Long hostId);

    List<ChargingSession> findAllByDriverIdOrderByStartTimeDesc(Long driverId);

    List<ChargingSession> findAllByChargerIdOrderByStartTimeDesc(Long chargerId);

    boolean existsByChargerIdAndStatus(Long chargerId, SessionStatus status);

    Optional<ChargingSession> findFirstByDriverIdAndStatus(Long driverId, SessionStatus status);

    Optional<ChargingSession> findBySessionReference(String sessionReference);

    long countByStatus(SessionStatus status);

    long countByStatusIn(java.util.Collection<SessionStatus> statuses);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(s.totalCost), 0) FROM ChargingSession s " +
        "WHERE s.charger.host.id = :hostId AND s.status IN (com.example.chargeconnect.session.model.SessionStatus.COMPLETED, com.example.chargeconnect.session.model.SessionStatus.EMERGENCY_STOPPED)"
    )
    java.math.BigDecimal sumTotalCostByHostId(@org.springframework.data.repository.query.Param("hostId") Long hostId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(s.energyDeliveredKwh), 0) FROM ChargingSession s")
    java.math.BigDecimal sumTotalEnergyDeliveredKwh();

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(s.totalCost), 0) FROM ChargingSession s " +
        "WHERE s.status IN (com.example.chargeconnect.session.model.SessionStatus.COMPLETED, com.example.chargeconnect.session.model.SessionStatus.EMERGENCY_STOPPED)"
    )
    java.math.BigDecimal sumTotalGrossVolume();
}
