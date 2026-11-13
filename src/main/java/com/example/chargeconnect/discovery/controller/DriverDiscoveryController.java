package com.example.chargeconnect.discovery.controller;

import com.example.chargeconnect.discovery.dto.VehicleMatchedChargerResponse;
import com.example.chargeconnect.discovery.service.DiscoveryService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver/discovery")
public class DriverDiscoveryController {

    private final DiscoveryService discoveryService;

    public DriverDiscoveryController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    /**
     * Driver clicks "Find Nearby" on the map: finds approved, available chargers
     * near their GPS location matching their vehicle's configuration.
     */
    @GetMapping("/nearby")
    public List<VehicleMatchedChargerResponse> findNearby(
            Authentication authentication,
            @RequestParam Long vehicleId,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false) Double radiusMeters,
            @RequestParam(required = false) BigDecimal maxPricePerKwh
    ) {
        return discoveryService.findCompatibleChargersForVehicle(
                authentication.getName(),
                vehicleId,
                latitude,
                longitude,
                radiusMeters,
                maxPricePerKwh
        );
    }
}
