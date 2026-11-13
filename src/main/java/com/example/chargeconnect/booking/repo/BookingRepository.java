package com.example.chargeconnect.booking.repo;

import com.example.chargeconnect.booking.model.Booking;
import com.example.chargeconnect.booking.model.BookingStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
        SELECT COUNT(b) > 0
        FROM Booking b
        WHERE b.charger.id = :chargerId
          AND b.status IN :activeStatuses
          AND b.startTime < :endTime
          AND b.endTime > :startTime
    """)
    boolean existsOverlapping(
            @Param("chargerId") Long chargerId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("activeStatuses") Collection<BookingStatus> activeStatuses
    );

    @Query("""
        SELECT b
        FROM Booking b
        WHERE b.charger.id = :chargerId
          AND b.status IN :activeStatuses
          AND b.startTime < :rangeEnd
          AND b.endTime > :rangeStart
        ORDER BY b.startTime ASC
    """)
    List<Booking> findActiveInDateRange(
            @Param("chargerId") Long chargerId,
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd,
            @Param("activeStatuses") Collection<BookingStatus> activeStatuses
    );

    List<Booking> findAllByDriverIdOrderByStartTimeDesc(Long driverId);

    Optional<Booking> findByIdAndDriverId(Long id, Long driverId);

    List<Booking> findAllByChargerIdOrderByStartTimeDesc(Long chargerId);

    Optional<Booking> findByIdAndChargerHostId(Long id, Long hostId);

    Optional<Booking> findByBookingReference(String bookingReference);
}

