package com.example.chargeconnect.booking.controller;

import com.example.chargeconnect.booking.dto.BookingResponse;
import com.example.chargeconnect.booking.dto.HostBlockSlotRequest;
import com.example.chargeconnect.booking.service.BookingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/host/chargers/{chargerId}")
public class HostBookingController {

    private final BookingService bookingService;

    public HostBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/bookings")
    public List<BookingResponse> getChargerBookings(
            Authentication authentication,
            @PathVariable Long chargerId
    ) {
        return bookingService.getHostChargerBookings(authentication.getName(), chargerId);
    }

    @PostMapping("/block-slots")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse blockSlot(
            Authentication authentication,
            @PathVariable Long chargerId,
            @Valid @RequestBody HostBlockSlotRequest request
    ) {
        return bookingService.blockSlotForHost(authentication.getName(), chargerId, request);
    }
}

