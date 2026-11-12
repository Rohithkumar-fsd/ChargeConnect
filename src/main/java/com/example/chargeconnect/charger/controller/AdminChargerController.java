package com.example.chargeconnect.charger.controller;

import com.example.chargeconnect.charger.dto.ChargerResponse;
import com.example.chargeconnect.charger.dto.VerificationStatus;
import com.example.chargeconnect.charger.dto.VerifyChargerRequest;
import com.example.chargeconnect.charger.service.ChargerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/chargers")
public class AdminChargerController {

    private final ChargerService chargerService;

    public AdminChargerController(ChargerService chargerService) {
        this.chargerService = chargerService;
    }

    @GetMapping
    public List<ChargerResponse> getChargers(
            @RequestParam(required = false) VerificationStatus status
    ) {
        return chargerService.getChargersForAdmin(status);
    }

    @GetMapping("/{chargerId}")
    public ChargerResponse getCharger(@PathVariable Long chargerId) {
        return chargerService.getChargerForAdmin(chargerId);
    }

    @PatchMapping("/{chargerId}/verification")
    public ChargerResponse verifyCharger(
            Authentication authentication,
            @PathVariable Long chargerId,
            @Valid @RequestBody VerifyChargerRequest request
    ) {
        return chargerService.verifyCharger(authentication.getName(), chargerId, request);
    }
}

