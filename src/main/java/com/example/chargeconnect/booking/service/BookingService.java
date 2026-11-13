package com.example.chargeconnect.booking.service;

import com.example.chargeconnect.booking.dto.*;
import com.example.chargeconnect.booking.model.Booking;
import com.example.chargeconnect.booking.model.BookingStatus;
import com.example.chargeconnect.booking.repo.BookingRepository;
import com.example.chargeconnect.charger.dto.AvailabilityStatus;
import com.example.chargeconnect.charger.dto.VerificationStatus;
import com.example.chargeconnect.charger.model.Charger;
import com.example.chargeconnect.charger.model.ChargerConnector;
import com.example.chargeconnect.charger.model.PowerType;
import com.example.chargeconnect.charger.repo.ChargerRepository;
import com.example.chargeconnect.exception.ResourceNotFoundException;
import com.example.chargeconnect.user.model.User;
import com.example.chargeconnect.user.repo.UserRepository;
import com.example.chargeconnect.vehicle.model.ConnectorType;
import com.example.chargeconnect.vehicle.model.Vehicle;
import com.example.chargeconnect.vehicle.model.VehicleConnector;
import com.example.chargeconnect.vehicle.repo.VehicleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingService {

    public static final Duration LOCK_TTL = Duration.ofMinutes(10);
    public static final Set<BookingStatus> ACTIVE_STATUSES = Set.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.BLOCKED_BY_HOST
    );

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ChargerRepository chargerRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingLockService lockService;

    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            ChargerRepository chargerRepository,
            VehicleRepository vehicleRepository,
            BookingLockService lockService
    ) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.chargerRepository = chargerRepository;
        this.vehicleRepository = vehicleRepository;
        this.lockService = lockService;
    }

    @Transactional
    public BookingResponse createBooking(String driverEmail, CreateBookingRequest request) {
        validateSlotInterval(request.startTime(), request.endTime());

        User driver = getUserByEmail(driverEmail);
        Charger charger = chargerRepository.findWithDetailsById(request.chargerId())
                .orElseThrow(() -> new ResourceNotFoundException("Charger not found"));

        if (charger.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Charger is not approved for bookings");
        }
        if (charger.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Charger is currently unavailable");
        }

        Vehicle vehicle = vehicleRepository.findByIdAndUserId(request.vehicleId(), driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found for authenticated driver"));

        validateConnectorCompatibility(charger, vehicle);
        validateWithinOperatingHours(charger, request.startTime(), request.endTime());

        // LAYER 1: Redis Distributed Lock
        String lockKey = buildLockKey(charger.getId(), request.startTime(), request.endTime());
        boolean acquired = lockService.tryAcquireLock(lockKey, driver.getEmail(), LOCK_TTL);
        if (!acquired) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This charging slot is currently being checked out by another driver. Please choose another slot."
            );
        }

        try {
            // LAYER 2: PostgreSQL Database Overlap Check
            boolean conflict = bookingRepository.existsOverlapping(
                    charger.getId(),
                    request.startTime(),
                    request.endTime(),
                    ACTIVE_STATUSES
            );

            if (conflict) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "The requested time slot conflicts with an existing booking"
                );
            }

            // Price & kWh Calculations
            long durationMinutes = Duration.between(request.startTime(), request.endTime()).toMinutes();
            BigDecimal totalHours = BigDecimal.valueOf(durationMinutes)
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

            BigDecimal effectivePowerKw = calculateEffectivePower(charger, vehicle);
            BigDecimal estimatedKwh = effectivePowerKw.multiply(totalHours).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalPrice = estimatedKwh.multiply(charger.getPricePerKwh()).setScale(2, RoundingMode.HALF_UP);

            Booking booking = new Booking(
                    driver,
                    charger,
                    vehicle,
                    request.startTime(),
                    request.endTime(),
                    totalHours,
                    charger.getPricePerKwh(),
                    estimatedKwh,
                    totalPrice,
                    BookingStatus.PENDING
            );

            Booking saved = bookingRepository.save(booking);
            return BookingResponse.from(saved);
        } catch (Exception ex) {
            lockService.releaseLock(lockKey, driver.getEmail());
            throw ex;
        }
    }

    @Transactional
    public BookingResponse confirmBooking(String driverEmail, Long bookingId) {
        User driver = getUserByEmail(driverEmail);
        Booking booking = bookingRepository.findByIdAndDriverId(bookingId, driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        booking.confirm();

        // Release checkout lock once permanently confirmed in PostgreSQL
        String lockKey = buildLockKey(booking.getCharger().getId(), booking.getStartTime(), booking.getEndTime());
        lockService.releaseLock(lockKey, driver.getEmail());

        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse cancelDriverBooking(String driverEmail, Long bookingId, CancelBookingRequest request) {
        User driver = getUserByEmail(driverEmail);
        Booking booking = bookingRepository.findByIdAndDriverId(bookingId, driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (Instant.now().isAfter(booking.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel a booking that has already started");
        }

        String reason = request != null && request.cancellationReason() != null
                ? request.cancellationReason().trim()
                : "Cancelled by driver";

        booking.cancel(reason);

        String lockKey = buildLockKey(booking.getCharger().getId(), booking.getStartTime(), booking.getEndTime());
        lockService.releaseLock(lockKey, driver.getEmail());

        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getDriverBookings(String driverEmail) {
        User driver = getUserByEmail(driverEmail);
        return bookingRepository.findAllByDriverIdOrderByStartTimeDesc(driver.getId())
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getDriverBooking(String driverEmail, Long bookingId) {
        User driver = getUserByEmail(driverEmail);
        Booking booking = bookingRepository.findByIdAndDriverId(bookingId, driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getHostChargerBookings(String hostEmail, Long chargerId) {
        User host = getUserByEmail(hostEmail);
        Charger charger = chargerRepository.findByIdAndHostId(chargerId, host.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Charger not found for authenticated host"));

        return bookingRepository.findAllByChargerIdOrderByStartTimeDesc(charger.getId())
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Transactional
    public BookingResponse blockSlotForHost(String hostEmail, Long chargerId, HostBlockSlotRequest request) {
        validateSlotInterval(request.startTime(), request.endTime());

        User host = getUserByEmail(hostEmail);
        Charger charger = chargerRepository.findByIdAndHostId(chargerId, host.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Charger not found for authenticated host"));

        boolean conflict = bookingRepository.existsOverlapping(
                charger.getId(),
                request.startTime(),
                request.endTime(),
                ACTIVE_STATUSES
        );

        if (conflict) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot block this time window because a confirmed or pending booking already exists"
            );
        }

        long durationMinutes = Duration.between(request.startTime(), request.endTime()).toMinutes();
        BigDecimal totalHours = BigDecimal.valueOf(durationMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        Booking block = new Booking(
                host,
                charger,
                null,
                request.startTime(),
                request.endTime(),
                totalHours,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BookingStatus.BLOCKED_BY_HOST
        );

        if (request.reason() != null && !request.reason().isBlank()) {
            block.setCancellationReason(request.reason().trim());
        }

        Booking saved = bookingRepository.save(block);
        return BookingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AvailableSlotsResponse getAvailableSlots(Long chargerId, LocalDate date) {
        Charger charger = chargerRepository.findById(chargerId)
                .orElseThrow(() -> new ResourceNotFoundException("Charger not found"));

        LocalTime openTime = charger.getOpenTime();
        LocalTime closeTime = charger.getCloseTime();

        Instant dayStart = date.atTime(0, 0).toInstant(ZoneOffset.UTC);
        Instant dayEnd = date.plusDays(1).atTime(0, 0).toInstant(ZoneOffset.UTC);

        List<Booking> activeBookings = bookingRepository.findActiveInDateRange(
                chargerId, dayStart, dayEnd, ACTIVE_STATUSES
        );

        List<TimeSlot> slots = new ArrayList<>();
        LocalTime current = openTime;

        // Generate 30-minute slot intervals within operating hours
        while (current.plusMinutes(30).isBefore(closeTime) || current.plusMinutes(30).equals(closeTime)) {
            LocalTime next = current.plusMinutes(30);
            Instant slotStart = date.atTime(current).toInstant(ZoneOffset.UTC);
            Instant slotEnd = date.atTime(next).toInstant(ZoneOffset.UTC);

            boolean isBusy = activeBookings.stream().anyMatch(b ->
                    b.getStartTime().isBefore(slotEnd) && b.getEndTime().isAfter(slotStart)
            );

            slots.add(new TimeSlot(slotStart, slotEnd, isBusy ? "BUSY" : "AVAILABLE"));

            if (next.equals(LocalTime.MIDNIGHT) || next.isBefore(current)) {
                break;
            }
            current = next;
        }

        return new AvailableSlotsResponse(charger.getId(), date, openTime, closeTime, slots);
    }

    private void validateSlotInterval(Instant start, Instant end) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be strictly before end time");
        }

        ZonedDateTime startUtc = start.atZone(ZoneOffset.UTC);
        ZonedDateTime endUtc = end.atZone(ZoneOffset.UTC);

        if ((startUtc.getMinute() != 0 && startUtc.getMinute() != 30) || startUtc.getSecond() != 0 || startUtc.getNano() != 0) {
            throw new IllegalArgumentException("Start time must be aligned to a 30-minute boundary (e.g. :00 or :30)");
        }
        if ((endUtc.getMinute() != 0 && endUtc.getMinute() != 30) || endUtc.getSecond() != 0 || endUtc.getNano() != 0) {
            throw new IllegalArgumentException("End time must be aligned to a 30-minute boundary (e.g. :00 or :30)");
        }

        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 30) {
            throw new IllegalArgumentException("Minimum booking duration is 30 minutes");
        }
        if (minutes % 30 != 0) {
            throw new IllegalArgumentException("Booking duration must be in multiples of 30 minutes (e.g. 30, 60, 90 mins)");
        }
    }

    private void validateWithinOperatingHours(Charger charger, Instant start, Instant end) {
        LocalTime open = charger.getOpenTime();
        LocalTime close = charger.getCloseTime();

        // 24/7 chargers skip operating hour checks
        if (open.equals(LocalTime.of(0, 0, 0)) && close.equals(LocalTime.of(23, 59, 59))) {
            return;
        }

        LocalTime startTime = start.atZone(ZoneOffset.UTC).toLocalTime();
        LocalTime endTime = end.atZone(ZoneOffset.UTC).toLocalTime();

        if (startTime.isBefore(open) || endTime.isAfter(close)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Requested time is outside charger operating hours (%s - %s UTC)", open, close)
            );
        }
    }

    private void validateConnectorCompatibility(Charger charger, Vehicle vehicle) {
        Set<ConnectorType> chargerConnectors = charger.getConnectors().stream()
                .map(ChargerConnector::getConnectorType)
                .collect(Collectors.toSet());

        boolean hasMatch = vehicle.getConnectors().stream()
                .map(VehicleConnector::getConnectorType)
                .anyMatch(chargerConnectors::contains);

        if (!hasMatch) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vehicle does not have any connector compatible with this charger"
            );
        }
    }

    private BigDecimal calculateEffectivePower(Charger charger, Vehicle vehicle) {
        BigDecimal chargerPower = charger.getMaxPowerKw();
        if (charger.getPowerType() == PowerType.AC) {
            return vehicle.getMaxAcPowerKw() != null
                    ? chargerPower.min(vehicle.getMaxAcPowerKw())
                    : chargerPower;
        }
        if (charger.getPowerType() == PowerType.DC) {
            return vehicle.getMaxDcPowerKw() != null
                    ? chargerPower.min(vehicle.getMaxDcPowerKw())
                    : chargerPower;
        }
        BigDecimal vehicleMax = null;
        if (vehicle.getMaxDcPowerKw() != null && vehicle.getMaxAcPowerKw() != null) {
            vehicleMax = vehicle.getMaxDcPowerKw().max(vehicle.getMaxAcPowerKw());
        } else if (vehicle.getMaxDcPowerKw() != null) {
            vehicleMax = vehicle.getMaxDcPowerKw();
        } else if (vehicle.getMaxAcPowerKw() != null) {
            vehicleMax = vehicle.getMaxAcPowerKw();
        }

        return vehicleMax != null ? chargerPower.min(vehicleMax) : chargerPower;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private String buildLockKey(Long chargerId, Instant start, Instant end) {
        return "lock:charger:" + chargerId + ":slot:" + start.toEpochMilli() + ":" + end.toEpochMilli();
    }
}
