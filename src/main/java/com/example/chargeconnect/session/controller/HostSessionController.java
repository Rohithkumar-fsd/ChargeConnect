package com.example.chargeconnect.session.controller;

import com.example.chargeconnect.session.dto.SessionResponse;
import com.example.chargeconnect.session.dto.StopSessionRequest;
import com.example.chargeconnect.session.service.ChargingSessionService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/host/chargers/{chargerId}/sessions")
public class HostSessionController {

    private final ChargingSessionService sessionService;

    public HostSessionController(ChargingSessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Host triggers an emergency stop for a charging session on their charger.
     */
    @PostMapping("/{sessionId}/emergency-stop")
    public SessionResponse emergencyStop(
            Authentication authentication,
            @PathVariable Long chargerId,
            @PathVariable Long sessionId,
            @RequestBody(required = false) StopSessionRequest request
    ) {
        return sessionService.emergencyStopByHost(authentication.getName(), chargerId, sessionId, request);
    }

    /**
     * Host views all charging sessions that occurred on this specific charger.
     */
    @GetMapping
    public List<SessionResponse> getChargerSessions(
            Authentication authentication,
            @PathVariable Long chargerId
    ) {
        return sessionService.getHostChargerSessions(authentication.getName(), chargerId);
    }
}