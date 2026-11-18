package com.example.chargeconnect.session.service;

import com.example.chargeconnect.booking.model.Booking;
import com.example.chargeconnect.booking.model.BookingStatus;
import com.example.chargeconnect.booking.repo.BookingRepository;
import com.example.chargeconnect.charger.dto.VerificationStatus;
import com.example.chargeconnect.charger.model.Charger;
import com.example.chargeconnect.charger.model.LocationType;
import com.example.chargeconnect.charger.model.PowerType;
import com.example.chargeconnect.charger.repo.ChargerRepository;
import com.example.chargeconnect.exception.ResourceNotFoundException;
import com.example.chargeconnect.session.dto.LiveTelemetryResponse;
import com.example.chargeconnect.session.dto.SessionResponse;
import com.example.chargeconnect.session.dto.StartSessionRequest;
import com.example.chargeconnect.session.dto.StopSessionRequest;
import com.example.chargeconnect.session.model.ChargingSession;
import com.example.chargeconnect.session.model.SessionStatus;
import com.example.chargeconnect.session.repo.ChargingSessionRepository;
import com.example.chargeconnect.user.model.User;
import com.example.chargeconnect.user.repo.UserRepository;
import com.example.chargeconnect.vehicle.model.Vehicle;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChargingSessionServiceTest {

    @Mock private ChargingSessionRepository sessionRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private ChargerRepository chargerRepository;
    @Mock private UserRepository userRepository;

    private ChargingSessionService sessionService;

    private User driver;
    private User host;
    private Charger charger;
    private Vehicle vehicle;
    private Booking booking;

    @BeforeEach
    void setUp() {
        sessionService = new ChargingSessionService(
                sessionRepository,
                bookingRepository,
                chargerRepository,
                userRepository
        );

        driver = new User("Rahul Driver", "driver@test.com", "+919876543210", "hash");
        setId(driver, 1L);

        host = new User("Kiran Host", "host@test.com", "+919876543211", "hash");
        setId(host, 2L);

        charger = new Charger(
                host, "Indiranagar Fast Hub", "50kW DC Charger", LocationType.COMMERCIAL,
                "100 Feet Rd", "Bengaluru", "Karnataka", "560038", "India",
                new BigDecimal("12.971600"), new BigDecimal("77.594600"),
                PowerType.DC, new BigDecimal("50.00"), new BigDecimal("15.00")
        );
        charger.approve(new User("Admin", "admin@test.com", null, "hash"), Instant.now());
        charger.setQrCode("CHG-QR-2");
        setId(charger, 2L);

        vehicle = new Vehicle(
                driver, "Tata", "Nexon EV", "KA-01-EV-1234",
                new BigDecimal("40.50"), new BigDecimal("7.20"), new BigDecimal("30.00")
        );
        setId(vehicle, 3L);

        Instant start = Instant.now().minus(10, ChronoUnit.MINUTES);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);
        booking = new Booking(
                driver, charger, vehicle, start, end,
                new BigDecimal("1.17"), new BigDecimal("15.00"), new BigDecimal("35.00"), new BigDecimal("525.00"),
                BookingStatus.CONFIRMED
        );
        setId(booking, 10L);
    }

    @Test
    void startSession_WithBookingId_SuccessfullyStartsActiveSession() {
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(chargerRepository.findByQrCode("CHG-QR-2")).thenReturn(Optional.of(charger));
        when(bookingRepository.findByIdAndDriverId(10L, driver.getId())).thenReturn(Optional.of(booking));
        when(sessionRepository.findByBookingId(10L)).thenReturn(Optional.empty());
        when(sessionRepository.existsByChargerIdAndStatus(2L, SessionStatus.ACTIVE)).thenReturn(false);
        when(sessionRepository.save(any(ChargingSession.class))).thenAnswer(inv -> {
            ChargingSession s = inv.getArgument(0);
            setId(s, 100L);
            return s;
        });

        SessionResponse response = sessionService.startSession(
                driver.getEmail(), new StartSessionRequest(10L, "CHG-QR-2")
        );

        assertThat(response.status()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(response.bookingId()).isEqualTo(10L);
        assertThat(response.chargerId()).isEqualTo(2L);
        assertThat(response.pricePerKwh()).isEqualByComparingTo("15.00");
    }

    @Test
    void startSession_WithoutBookingId_ResolvesConfirmedBookingAutomatically() {
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(chargerRepository.findByQrCode("CHG-QR-2")).thenReturn(Optional.of(charger));
        when(bookingRepository.findFirstByDriverIdAndChargerIdAndStatusOrderByStartTimeAsc(
                driver.getId(), 2L, BookingStatus.CONFIRMED
        )).thenReturn(Optional.of(booking));
        when(sessionRepository.findByBookingId(10L)).thenReturn(Optional.empty());
        when(sessionRepository.existsByChargerIdAndStatus(2L, SessionStatus.ACTIVE)).thenReturn(false);
        when(sessionRepository.save(any(ChargingSession.class))).thenAnswer(inv -> {
            ChargingSession s = inv.getArgument(0);
            setId(s, 101L);
            return s;
        });

        SessionResponse response = sessionService.startSession(
                driver.getEmail(), new StartSessionRequest(null, "CHG-QR-2")
        );

        assertThat(response.status()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(response.bookingId()).isEqualTo(10L);
    }

    @Test
    void startSession_InvalidQrCode_Throws404() {
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(chargerRepository.findByQrCode("INVALID-QR")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.startSession(driver.getEmail(), new StartSessionRequest(10L, "INVALID-QR")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invalid QR code");
    }

    @Test
    void startSession_BookingNotConfirmed_Throws400() {
        Booking pendingBooking = new Booking(
                driver, charger, vehicle, Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS),
                new BigDecimal("1.00"), new BigDecimal("15.00"), new BigDecimal("30.00"), new BigDecimal("450.00"),
                BookingStatus.PENDING
        );
        setId(pendingBooking, 11L);

        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(chargerRepository.findByQrCode("CHG-QR-2")).thenReturn(Optional.of(charger));
        when(bookingRepository.findByIdAndDriverId(11L, driver.getId())).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> sessionService.startSession(driver.getEmail(), new StartSessionRequest(11L, "CHG-QR-2")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only CONFIRMED bookings can be started");
    }

    @Test
    void startSession_ChargerAlreadyOccupied_Throws409() {
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(chargerRepository.findByQrCode("CHG-QR-2")).thenReturn(Optional.of(charger));
        when(bookingRepository.findByIdAndDriverId(10L, driver.getId())).thenReturn(Optional.of(booking));
        when(sessionRepository.findByBookingId(10L)).thenReturn(Optional.empty());
        when(sessionRepository.existsByChargerIdAndStatus(2L, SessionStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> sessionService.startSession(driver.getEmail(), new StartSessionRequest(10L, "CHG-QR-2")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Charger is currently in use");
    }

    @Test
    void getLiveTelemetry_CalculatesLiveMetrics() {
        ChargingSession session = new ChargingSession(booking, driver, charger, vehicle, BigDecimal.ZERO);
        setId(session, 100L);

        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(sessionRepository.findByIdAndDriverId(100L, driver.getId())).thenReturn(Optional.of(session));

        LiveTelemetryResponse telemetry = sessionService.getLiveTelemetry(driver.getEmail(), 100L);

        assertThat(telemetry.sessionId()).isEqualTo(100L);
        assertThat(telemetry.status()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(telemetry.currentPowerKw()).isEqualByComparingTo("50.00");
    }

    @Test
    void stopSessionByDriver_SuccessfullyCompletesSessionAndBooking() {
        ChargingSession session = new ChargingSession(booking, driver, charger, vehicle, BigDecimal.ZERO);
        setId(session, 100L);

        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(sessionRepository.findByIdAndDriverId(100L, driver.getId())).thenReturn(Optional.of(session));
        when(sessionRepository.save(session)).thenReturn(session);
        when(bookingRepository.save(booking)).thenReturn(booking);

        SessionResponse response = sessionService.stopSessionByDriver(
                driver.getEmail(), 100L, new StopSessionRequest("Battery full")
        );

        assertThat(response.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(response.stopReason()).isEqualTo("Battery full");
    }

    @Test
    void emergencyStopByHost_SuccessfullyStopsWithEmergencyStatus() {
        ChargingSession session = new ChargingSession(booking, driver, charger, vehicle, BigDecimal.ZERO);
        setId(session, 100L);

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(chargerRepository.findByIdAndHostId(2L, host.getId())).thenReturn(Optional.of(charger));
        when(sessionRepository.findByIdAndChargerHostId(100L, host.getId())).thenReturn(Optional.of(session));
        when(sessionRepository.save(session)).thenReturn(session);
        when(bookingRepository.save(booking)).thenReturn(booking);

        SessionResponse response = sessionService.emergencyStopByHost(
                host.getEmail(), 2L, 100L, new StopSessionRequest("Electrical spark detected")
        );

        assertThat(response.status()).isEqualTo(SessionStatus.EMERGENCY_STOPPED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(response.stopReason()).isEqualTo("Electrical spark detected");
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set id via reflection", e);
        }
    }
}
