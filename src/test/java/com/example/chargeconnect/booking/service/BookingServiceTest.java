package com.example.chargeconnect.booking.service;

import com.example.chargeconnect.booking.dto.CancelBookingRequest;
import com.example.chargeconnect.booking.dto.CreateBookingRequest;
import com.example.chargeconnect.booking.dto.HostBlockSlotRequest;
import com.example.chargeconnect.booking.model.Booking;
import com.example.chargeconnect.booking.model.BookingStatus;
import com.example.chargeconnect.booking.repo.BookingRepository;
import com.example.chargeconnect.charger.dto.AvailabilityStatus;
import com.example.chargeconnect.charger.dto.VerificationStatus;
import com.example.chargeconnect.charger.model.Charger;
import com.example.chargeconnect.charger.model.LocationType;
import com.example.chargeconnect.charger.model.PowerType;
import com.example.chargeconnect.charger.repo.ChargerRepository;
import com.example.chargeconnect.user.model.User;
import com.example.chargeconnect.user.repo.UserRepository;
import com.example.chargeconnect.vehicle.model.ConnectorType;
import com.example.chargeconnect.vehicle.model.Vehicle;
import com.example.chargeconnect.vehicle.repo.VehicleRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChargerRepository chargerRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private BookingLockService lockService;

    private BookingService bookingService;

    private User driver;
    private User host;
    private Charger charger;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository,
                userRepository,
                chargerRepository,
                vehicleRepository,
                lockService
        );

        driver = new User("Driver Dave", "driver@test.com", "+919876543210", "hash");
        host = new User("Host Helen", "host@test.com", "+919876543211", "hash");

        charger = new Charger(
                host,
                "Central Fast Charger",
                "Fast charging",
                LocationType.COMMERCIAL,
                "123 Street",
                "City",
                "State",
                "560001",
                "India",
                new BigDecimal("12.971600"),
                new BigDecimal("77.594600"),
                PowerType.DC,
                new BigDecimal("50.00"),
                new BigDecimal("15.00")
        );
        charger.approve(new User("Admin", "admin@test.com", null, "hash"), Instant.now());
        charger.replaceConnectors(Set.of(ConnectorType.CCS2));

        vehicle = new Vehicle(
                driver,
                "Tata",
                "Nexon EV",
                "KA-01-EV-1234",
                new BigDecimal("40.50"),
                new BigDecimal("7.20"),
                new BigDecimal("30.00")
        );
        vehicle.replaceConnectors(Set.of(ConnectorType.CCS2));
    }

    @Test
    void createBookingSuccessfullyAcquiresLockAndSavesPendingBooking() {
        Instant start = LocalDate.now().plusDays(1).atTime(10, 0).toInstant(ZoneOffset.UTC);
        Instant end = LocalDate.now().plusDays(1).atTime(12, 0).toInstant(ZoneOffset.UTC); // 2 hours

        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(chargerRepository.findWithDetailsById(1L)).thenReturn(Optional.of(charger));
        when(vehicleRepository.findByIdAndUserId(10L, driver.getId())).thenReturn(Optional.of(vehicle));
        when(lockService.tryAcquireLock(any(), eq(driver.getEmail()), any())).thenReturn(true);
        when(bookingRepository.existsOverlapping(eq(charger.getId()), eq(start), eq(end), any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = bookingService.createBooking(driver.getEmail(), new CreateBookingRequest(1L, 10L, start, end));

        assertThat(response.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.totalHours()).isEqualByComparingTo("2.00");
        // Effective power is min(charger 50kW, car DC 30kW) = 30kW
        // estimatedKwh = 30 * 2 = 60.00 kWh
        assertThat(response.estimatedKwh()).isEqualByComparingTo("60.00");
        // totalPrice = 60 kWh * 15.00 = 900.00
        assertThat(response.totalPrice()).isEqualByComparingTo("900.00");

        verify(lockService).tryAcquireLock(any(), eq(driver.getEmail()), any());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBookingFailsWhenLockCannotBeAcquired() {
        Instant start = LocalDate.now().plusDays(1).atTime(10, 0).toInstant(ZoneOffset.UTC);
        Instant end = LocalDate.now().plusDays(1).atTime(11, 0).toInstant(ZoneOffset.UTC);

        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(chargerRepository.findWithDetailsById(1L)).thenReturn(Optional.of(charger));
        when(vehicleRepository.findByIdAndUserId(10L, driver.getId())).thenReturn(Optional.of(vehicle));
        when(lockService.tryAcquireLock(any(), eq(driver.getEmail()), any())).thenReturn(false);

        assertThatThrownBy(() ->
                bookingService.createBooking(driver.getEmail(), new CreateBookingRequest(1L, 10L, start, end))
        ).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("currently being checked out by another driver");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingReleasesLockWhenDatabaseConflictOccurs() {
        Instant start = LocalDate.now().plusDays(1).atTime(10, 0).toInstant(ZoneOffset.UTC);
        Instant end = LocalDate.now().plusDays(1).atTime(11, 0).toInstant(ZoneOffset.UTC);

        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(chargerRepository.findWithDetailsById(1L)).thenReturn(Optional.of(charger));
        when(vehicleRepository.findByIdAndUserId(10L, driver.getId())).thenReturn(Optional.of(vehicle));
        when(lockService.tryAcquireLock(any(), eq(driver.getEmail()), any())).thenReturn(true);
        when(bookingRepository.existsOverlapping(eq(charger.getId()), eq(start), eq(end), any())).thenReturn(true);

        assertThatThrownBy(() ->
                bookingService.createBooking(driver.getEmail(), new CreateBookingRequest(1L, 10L, start, end))
        ).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("conflicts with an existing booking");

        verify(lockService).releaseLock(any(), eq(driver.getEmail()));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingValidates30MinuteGranularity() {
        Instant start = LocalDate.now().plusDays(1).atTime(10, 15).toInstant(ZoneOffset.UTC); // :15 is invalid
        Instant end = LocalDate.now().plusDays(1).atTime(11, 0).toInstant(ZoneOffset.UTC);

        assertThatThrownBy(() ->
                bookingService.createBooking(driver.getEmail(), new CreateBookingRequest(1L, 10L, start, end))
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aligned to a 30-minute boundary");
    }

    @Test
    void confirmBookingTransitionsStatusAndReleasesLock() {
        Instant start = LocalDate.now().plusDays(1).atTime(10, 0).toInstant(ZoneOffset.UTC);
        Instant end = LocalDate.now().plusDays(1).atTime(11, 0).toInstant(ZoneOffset.UTC);

        Booking booking = new Booking(
                driver, charger, vehicle, start, end,
                new BigDecimal("1.00"), new BigDecimal("15.00"), new BigDecimal("30.00"), new BigDecimal("450.00"),
                BookingStatus.PENDING
        );

        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(bookingRepository.findByIdAndDriverId(100L, driver.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        var response = bookingService.confirmBooking(driver.getEmail(), 100L);

        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        verify(lockService).releaseLock(any(), eq(driver.getEmail()));
    }

    @Test
    void cancelDriverBookingSuccessfully() {
        Instant start = LocalDate.now().plusDays(1).atTime(10, 0).toInstant(ZoneOffset.UTC);
        Instant end = LocalDate.now().plusDays(1).atTime(11, 0).toInstant(ZoneOffset.UTC);

        Booking booking = new Booking(
                driver, charger, vehicle, start, end,
                new BigDecimal("1.00"), new BigDecimal("15.00"), new BigDecimal("30.00"), new BigDecimal("450.00"),
                BookingStatus.CONFIRMED
        );

        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(bookingRepository.findByIdAndDriverId(100L, driver.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        var response = bookingService.cancelDriverBooking(driver.getEmail(), 100L, new CancelBookingRequest("Plans changed"));

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(response.cancellationReason()).isEqualTo("Plans changed");
    }

    @Test
    void blockSlotForHostCreatesHostBlockBooking() {
        Instant start = LocalDate.now().plusDays(1).atTime(9, 0).toInstant(ZoneOffset.UTC);
        Instant end = LocalDate.now().plusDays(1).atTime(10, 0).toInstant(ZoneOffset.UTC);

        when(userRepository.findByEmail(host.getEmail())).thenReturn(Optional.of(host));
        when(chargerRepository.findByIdAndHostId(1L, host.getId())).thenReturn(Optional.of(charger));
        when(bookingRepository.existsOverlapping(eq(charger.getId()), eq(start), eq(end), any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = bookingService.blockSlotForHost(
                host.getEmail(),
                1L,
                new HostBlockSlotRequest(start, end, "Personal EV charging")
        );

        assertThat(response.status()).isEqualTo(BookingStatus.BLOCKED_BY_HOST);
        assertThat(response.totalHours()).isEqualByComparingTo("1.00");
    }

    @Test
    void getAvailableSlotsReturns30MinuteIntervals() {
        LocalDate date = LocalDate.now().plusDays(1);
        charger.setOpenTime(LocalTime.of(10, 0));
        charger.setCloseTime(LocalTime.of(12, 0)); // 2 hours = 4 30-min slots: 10:00-10:30, 10:30-11:00, 11:00-11:30, 11:30-12:00

        when(chargerRepository.findById(1L)).thenReturn(Optional.of(charger));

        // Create 1 active booking from 10:30 to 11:30
        Instant bookedStart = date.atTime(10, 30).toInstant(ZoneOffset.UTC);
        Instant bookedEnd = date.atTime(11, 30).toInstant(ZoneOffset.UTC);
        Booking active = new Booking(
                driver, charger, vehicle, bookedStart, bookedEnd,
                new BigDecimal("1.00"), new BigDecimal("15.00"), new BigDecimal("30.00"), new BigDecimal("450.00"),
                BookingStatus.CONFIRMED
        );

        when(bookingRepository.findActiveInDateRange(eq(1L), any(), any(), any())).thenReturn(List.of(active));

        var response = bookingService.getAvailableSlots(1L, date);

        assertThat(response.slots()).hasSize(4);
        assertThat(response.slots().get(0).status()).isEqualTo("AVAILABLE"); // 10:00 - 10:30
        assertThat(response.slots().get(1).status()).isEqualTo("BUSY");      // 10:30 - 11:00
        assertThat(response.slots().get(2).status()).isEqualTo("BUSY");      // 11:00 - 11:30
        assertThat(response.slots().get(3).status()).isEqualTo("AVAILABLE"); // 11:30 - 12:00
    }
}

