package com.example.chargeconnect.session.controller;

import com.example.chargeconnect.session.dto.*;
import com.example.chargeconnect.session.service.ChargingSessionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver/sessions")
public class DriverSessionController {

    private final ChargingSessionService sessionService;

    public DriverSessionController(ChargingSessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Driver scans QR code to start charging.
     */
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse startSession(
            Authentication authentication,
            @Valid @RequestBody StartSessionRequest request
    ) {
        return sessionService.startSession(authentication.getName(), request);
    }

    /**
     * Driver stops their active charging session.
     */
    @PostMapping("/{sessionId}/stop")
    public SessionResponse stopSession(
            Authentication authentication,
            @PathVariable Long sessionId,
            @RequestBody(required = false) StopSessionRequest request
    ) {
        return sessionService.stopSessionByDriver(authentication.getName(), sessionId, request);
    }

    /**
     * Live telemetry for active charging screen.
     */
    @GetMapping("/{sessionId}/telemetry")
    public LiveTelemetryResponse getLiveTelemetry(
            Authentication authentication,
            @PathVariable Long sessionId
    ) {
        return sessionService.getLiveTelemetry(authentication.getName(), sessionId);
    }

    /**
     * Fetch active session for the authenticated driver (if any).
     */
    @GetMapping("/active")
    public SessionResponse getActiveSession(Authentication authentication) {
        return sessionService.getActiveSessionForDriver(authentication.getName());
    }

    /**
     * Fetch specific session details.
     */
    @GetMapping("/{sessionId}")
    public SessionResponse getSession(
            Authentication authentication,
            @PathVariable Long sessionId
    ) {
        return sessionService.getSessionForDriver(authentication.getName(), sessionId);
    }

    /**
     * List all charging sessions for authenticated driver.
     */
    @GetMapping
    public List<SessionResponse> getMySessions(Authentication authentication) {
        return sessionService.getDriverSessions(authentication.getName());
    }
}