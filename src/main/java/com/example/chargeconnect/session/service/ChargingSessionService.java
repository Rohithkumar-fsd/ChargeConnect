package com.example.chargeconnect.session.service;

import com.example.chargeconnect.booking.model.Booking;
import com.example.chargeconnect.booking.model.BookingStatus;
import com.example.chargeconnect.booking.repo.BookingRepository;
import com.example.chargeconnect.charger.dto.VerificationStatus;
import com.example.chargeconnect.charger.model.Charger;
import com.example.chargeconnect.charger.repo.ChargerRepository;
import com.example.chargeconnect.exception.ResourceNotFoundException;
import com.example.chargeconnect.session.dto.*;
import com.example.chargeconnect.session.model.ChargingSession;
import com.example.chargeconnect.session.model.SessionStatus;
import com.example.chargeconnect.session.repo.ChargingSessionRepository;
import com.example.chargeconnect.user.model.User;
import com.example.chargeconnect.user.repo.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChargingSessionService {

    private static final Logger log = LoggerFactory.getLogger(ChargingSessionService.class);

    private final ChargingSessionRepository sessionRepository;
    private final BookingRepository bookingRepository;
    private final ChargerRepository chargerRepository;
    private final UserRepository userRepository;

    public ChargingSessionService(
            ChargingSessionRepository sessionRepository,
            BookingRepository bookingRepository,
            ChargerRepository chargerRepository,
            UserRepository userRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.bookingRepository = bookingRepository;
        this.chargerRepository = chargerRepository;
        this.userRepository = userRepository;
    }

    /**
     * Driver scans charger QR code to start charging.
     */
    @Transactional
    public SessionResponse startSession(String driverEmail, StartSessionRequest request) {
        User driver = getUserByEmail(driverEmail);

        // 1. Verify Charger exists by QR code
        Charger charger = chargerRepository.findByQrCode(request.qrCode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid QR code: no charger found for " + request.qrCode()));

        if (charger.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Charger is not approved for public charging");
        }

        // 2. Resolve Booking (either by specified ID or automatic lookup)
        Booking booking;
        if (request.bookingId() != null) {
            booking = bookingRepository.findByIdAndDriverId(request.bookingId(), driver.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found for authenticated driver"));

            if (!booking.getCharger().getId().equals(charger.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Scanned QR code belongs to charger '" + charger.getTitle() + "', but your booking is for a different charger"
                );
            }
        } else {
            booking = bookingRepository.findFirstByDriverIdAndChargerIdAndStatusOrderByStartTimeAsc(
                    driver.getId(), charger.getId(), BookingStatus.CONFIRMED
            ).orElseThrow(() -> new ResourceNotFoundException(
                    "No CONFIRMED booking found for you at this charger"
            ));
        }

        // 3. Booking must be CONFIRMED (paid for)
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only CONFIRMED bookings can be started. Current booking status: " + booking.getStatus()
            );
        }

        // 4. Booking slot must not have expired
        Instant now = Instant.now();
        if (now.isAfter(booking.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking time slot has already ended");
        }

        // 5. Ensure no existing session for this booking
        if (sessionRepository.findByBookingId(booking.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A charging session has already been started for this booking");
        }

        // 6. Ensure charger is not currently occupied by another active session
        if (sessionRepository.existsByChargerIdAndStatus(charger.getId(), SessionStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Charger is currently in use by another active session");
        }

        // 7. Create and persist session
        ChargingSession session = new ChargingSession(
                booking, driver, charger, booking.getVehicle(), BigDecimal.ZERO
        );

        ChargingSession saved = sessionRepository.save(session);
        log.info("Charging session '{}' started for booking '{}' at charger '{}'",
                saved.getSessionReference(), booking.getId(), charger.getId());

        return SessionResponse.from(saved);
    }

    /**
     * Live telemetry for active charging session.
     */
    @Transactional(readOnly = true)
    public LiveTelemetryResponse getLiveTelemetry(String driverEmail, Long sessionId) {
        User driver = getUserByEmail(driverEmail);
        ChargingSession session = sessionRepository.findByIdAndDriverId(sessionId, driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Charging session not found"));

        return LiveTelemetryResponse.from(session);
    }

    /**
     * Driver stops their charging session.
     */
    @Transactional
    public SessionResponse stopSessionByDriver(String driverEmail, Long sessionId, StopSessionRequest request) {
        User driver = getUserByEmail(driverEmail);
        ChargingSession session = sessionRepository.findByIdAndDriverId(sessionId, driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Charging session not found for authenticated driver"));

        return stopSessionInternal(session, request != null ? request.reason() : "Driver requested stop", false);
    }

    /**
     * Host triggers an emergency stop for a session on their charger.
     */
    @Transactional
    public SessionResponse emergencyStopByHost(String hostEmail, Long chargerId, Long sessionId, StopSessionRequest request) {
        User host = getUserByEmail(hostEmail);
        Charger charger = chargerRepository.findByIdAndHostId(chargerId, host.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Charger not found for authenticated host"));

        ChargingSession session = sessionRepository.findByIdAndChargerHostId(sessionId, host.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Charging session not found on this charger"));

        if (!session.getCharger().getId().equals(charger.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session does not belong to the specified charger");
        }

        return stopSessionInternal(session, request != null && request.reason() != null
                ? request.reason() : "Emergency stop invoked by host", true);
    }

    /**
     * Shared logic to complete/stop a session and finalize the booking.
     */
    private SessionResponse stopSessionInternal(ChargingSession session, String reason, boolean isEmergency) {
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session is not ACTIVE. Current status: " + session.getStatus());
        }

        if (isEmergency) {
            session.emergencyStop(reason);
        } else {
            session.complete(Instant.now(), reason);
        }

        // Finalize booking
        Booking booking = session.getBooking();
        booking.complete();
        bookingRepository.save(booking);

        ChargingSession saved = sessionRepository.save(session);
        log.info("Charging session '{}' {} with {} kWh delivered (cost: ₹{})",
                saved.getSessionReference(), saved.getStatus(), saved.getEnergyDeliveredKwh(), saved.getTotalCost());

        return SessionResponse.from(saved);
    }

    /**
     * Get specific session details for driver.
     */
    @Transactional(readOnly = true)
    public SessionResponse getSessionForDriver(String driverEmail, Long sessionId) {
        User driver = getUserByEmail(driverEmail);
        ChargingSession session = sessionRepository.findByIdAndDriverId(sessionId, driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Charging session not found"));
        return SessionResponse.from(session);
    }

    /**
     * Get driver's active session (if any).
     */
    @Transactional(readOnly = true)
    public SessionResponse getActiveSessionForDriver(String driverEmail) {
        User driver = getUserByEmail(driverEmail);
        ChargingSession session = sessionRepository.findFirstByDriverIdAndStatus(driver.getId(), SessionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active charging session found for driver"));
        return SessionResponse.from(session);
    }

    /**
     * List all charging sessions for authenticated driver.
     */
    @Transactional(readOnly = true)
    public List<SessionResponse> getDriverSessions(String driverEmail) {
        User driver = getUserByEmail(driverEmail);
        return sessionRepository.findAllByDriverIdOrderByStartTimeDesc(driver.getId()).stream()
                .map(SessionResponse::from)
                .toList();
    }

    /**
     * List all charging sessions on a host's charger.
     */
    @Transactional(readOnly = true)
    public List<SessionResponse> getHostChargerSessions(String hostEmail, Long chargerId) {
        User host = getUserByEmail(hostEmail);
        Charger charger = chargerRepository.findByIdAndHostId(chargerId, host.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Charger not found for authenticated host"));

        return sessionRepository.findAllByChargerIdOrderByStartTimeDesc(charger.getId()).stream()
                .map(SessionResponse::from)
                .toList();
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}