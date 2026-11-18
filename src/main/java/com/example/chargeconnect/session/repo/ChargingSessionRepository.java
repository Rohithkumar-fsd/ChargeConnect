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
}
