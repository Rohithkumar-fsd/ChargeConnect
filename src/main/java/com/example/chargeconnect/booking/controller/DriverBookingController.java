package com.example.chargeconnect.booking.controller;

import com.example.chargeconnect.booking.dto.*;
import com.example.chargeconnect.booking.service.BookingService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver/bookings")
public class DriverBookingController {

    private final BookingService bookingService;

    public DriverBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(
            Authentication authentication,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        return bookingService.createBooking(authentication.getName(), request);
    }

    @PostMapping("/{bookingId}/confirm")
    public BookingResponse confirmBooking(
            Authentication authentication,
            @PathVariable Long bookingId
    ) {
        return bookingService.confirmBooking(authentication.getName(), bookingId);
    }

    @PostMapping("/{bookingId}/cancel")
    public BookingResponse cancelBooking(
            Authentication authentication,
            @PathVariable Long bookingId,
            @RequestBody(required = false) CancelBookingRequest request
    ) {
        return bookingService.cancelDriverBooking(authentication.getName(), bookingId, request);
    }

    @GetMapping
    public List<BookingResponse> getMyBookings(Authentication authentication) {
        return bookingService.getDriverBookings(authentication.getName());
    }

    @GetMapping("/{bookingId}")
    public BookingResponse getMyBooking(
            Authentication authentication,
            @PathVariable Long bookingId
    ) {
        return bookingService.getDriverBooking(authentication.getName(), bookingId);
    }

    @GetMapping("/available-slots")
    public AvailableSlotsResponse getAvailableSlots(
            @RequestParam Long chargerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return bookingService.getAvailableSlots(chargerId, date);
    }
}

