package com.example.chargeconnect.charger.controller;

import com.example.chargeconnect.charger.dto.ChargerPatchRequest;
import com.example.chargeconnect.charger.dto.ChargerRequest;
import com.example.chargeconnect.charger.dto.ChargerResponse;
import com.example.chargeconnect.charger.dto.UpdateAvailabilityRequest;
import com.example.chargeconnect.charger.service.ChargerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/host/chargers")
public class HostChargerController {

    private final ChargerService chargerService;

    public HostChargerController(ChargerService chargerService) {
        this.chargerService = chargerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChargerResponse createCharger(
            Authentication authentication,
            @Valid @RequestBody ChargerRequest request
    ) {
        return chargerService.createCharger(authentication.getName(), request);
    }

    @GetMapping
    public List<ChargerResponse> getMyChargers(Authentication authentication) {
        return chargerService.getMyChargers(authentication.getName());
    }

    @GetMapping("/{chargerId}")
    public ChargerResponse getMyCharger(
            Authentication authentication,
            @PathVariable Long chargerId
    ) {
        return chargerService.getMyCharger(authentication.getName(), chargerId);
    }

    @PatchMapping("/{chargerId}")
    public ChargerResponse updateCharger(
            Authentication authentication,
            @PathVariable Long chargerId,
            @Valid @RequestBody ChargerPatchRequest request
    ) {
        return chargerService.updateCharger(authentication.getName(), chargerId, request);
    }

    @PatchMapping("/{chargerId}/availability")
    public ChargerResponse updateAvailability(
            Authentication authentication,
            @PathVariable Long chargerId,
            @Valid @RequestBody UpdateAvailabilityRequest request
    ) {
        return chargerService.updateAvailability(authentication.getName(), chargerId, request);
    }

    @DeleteMapping("/{chargerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCharger(
            Authentication authentication,
            @PathVariable Long chargerId
    ) {
        chargerService.deleteCharger(authentication.getName(), chargerId);
    }
}

