package com.example.chargeconnect.vehicle.controller;

import com.example.chargeconnect.vehicle.dto.VehiclePatchRequest;
import com.example.chargeconnect.vehicle.dto.VehicleRequest;
import com.example.chargeconnect.vehicle.dto.VehicleResponse;
import com.example.chargeconnect.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse createVehicle(
            Authentication authentication,
            @Valid @RequestBody VehicleRequest request
    ) {
        return vehicleService.createVehicle(authentication.getName(), request);
    }

    @GetMapping
    public List<VehicleResponse> getMyVehicles(Authentication authentication) {
        return vehicleService.getMyVehicles(authentication.getName());
    }

    @GetMapping("/{vehicleId}")
    public VehicleResponse getMyVehicle(
            Authentication authentication,
            @PathVariable Long vehicleId
    ) {
        return vehicleService.getMyVehicle(authentication.getName(), vehicleId);
    }

    @PatchMapping("/{vehicleId}")
    public VehicleResponse updateVehicle(
            Authentication authentication,
            @PathVariable Long vehicleId,
            @Valid @RequestBody VehiclePatchRequest request
    ) {
        return vehicleService.updateVehicle(authentication.getName(), vehicleId, request);
    }

    @DeleteMapping("/{vehicleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVehicle(
            Authentication authentication,
            @PathVariable Long vehicleId
    ) {
        vehicleService.deleteVehicle(authentication.getName(), vehicleId);
    }
}

