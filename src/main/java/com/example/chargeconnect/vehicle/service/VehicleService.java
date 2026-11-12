package com.example.chargeconnect.vehicle.service;

import com.example.chargeconnect.exception.DuplicateResourceException;
import com.example.chargeconnect.exception.ResourceNotFoundException;
import com.example.chargeconnect.user.model.User;
import com.example.chargeconnect.user.repo.UserRepository;
import com.example.chargeconnect.vehicle.dto.VehiclePatchRequest;
import com.example.chargeconnect.vehicle.dto.VehicleRequest;
import com.example.chargeconnect.vehicle.dto.VehicleResponse;
import com.example.chargeconnect.vehicle.model.Vehicle;
import com.example.chargeconnect.vehicle.repo.VehicleRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public VehicleResponse createVehicle(String userEmail, VehicleRequest request) {
        User owner = getAuthenticatedUser(userEmail);
        String normalizedRegistration = normalizeRegistrationNumber(request.registrationNumber());

        if (normalizedRegistration != null
                && vehicleRepository.existsByRegistrationNumber(normalizedRegistration)) {
            throw new DuplicateResourceException("A vehicle with this registration number already exists");
        }

        Vehicle vehicle = new Vehicle(
                owner,
                request.manufacturer().trim(),
                request.model().trim(),
                normalizedRegistration,
                request.batteryCapacityKwh(),
                request.maxAcPowerKw(),
                request.maxDcPowerKw()
        );
        vehicle.replaceConnectors(request.connectorTypes());

        Vehicle saved = vehicleRepository.saveAndFlush(vehicle);
        return VehicleResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyVehicles(String userEmail) {
        User owner = getAuthenticatedUser(userEmail);
        return vehicleRepository.findAllByUserIdOrderByCreatedAtDesc(owner.getId())
                .stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse getMyVehicle(String userEmail, Long vehicleId) {
        User owner = getAuthenticatedUser(userEmail);
        Vehicle vehicle = getOwnedVehicle(vehicleId, owner.getId());
        return VehicleResponse.from(vehicle);
    }

    @Transactional
    public VehicleResponse updateVehicle(String userEmail, Long vehicleId, VehiclePatchRequest request) {
        User owner = getAuthenticatedUser(userEmail);
        Vehicle vehicle = getOwnedVehicle(vehicleId, owner.getId());

        if (request.manufacturer() != null) {
            vehicle.setManufacturer(requireNonBlank(request.manufacturer(), "Manufacturer"));
        }
        if (request.model() != null) {
            vehicle.setModel(requireNonBlank(request.model(), "Model"));
        }
        if (request.registrationNumber() != null) {
            String normalizedRegistration = normalizeRegistrationNumber(request.registrationNumber());
            if (normalizedRegistration != null
                    && vehicleRepository.existsByRegistrationNumberAndIdNot(normalizedRegistration, vehicle.getId())) {
                throw new DuplicateResourceException("A vehicle with this registration number already exists");
            }
            vehicle.setRegistrationNumber(normalizedRegistration);
        }
        if (request.batteryCapacityKwh() != null) {
            vehicle.setBatteryCapacityKwh(request.batteryCapacityKwh());
        }
        if (request.maxAcPowerKw() != null) {
            vehicle.setMaxAcPowerKw(request.maxAcPowerKw());
        }
        if (request.maxDcPowerKw() != null) {
            vehicle.setMaxDcPowerKw(request.maxDcPowerKw());
        }
        if (request.connectorTypes() != null) {
            if (request.connectorTypes().isEmpty()) {
                throw new IllegalArgumentException("At least one connector type is required");
            }
            vehicle.replaceConnectors(request.connectorTypes());
        }

        // Flush so @PreUpdate refreshes updatedAt before building the response DTO.
        Vehicle updated = vehicleRepository.saveAndFlush(vehicle);
        return VehicleResponse.from(updated);
    }

    @Transactional
    public void deleteVehicle(String userEmail, Long vehicleId) {
        User owner = getAuthenticatedUser(userEmail);
        Vehicle vehicle = getOwnedVehicle(vehicleId, owner.getId());
        vehicleRepository.delete(vehicle);
    }

    private User getAuthenticatedUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private Vehicle getOwnedVehicle(Long vehicleId, Long userId) {
        return vehicleRepository.findByIdAndUserId(vehicleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
    }

    private String requireNonBlank(String value, String fieldName) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return trimmed;
    }

    private String normalizeRegistrationNumber(String registrationNumber) {
        if (registrationNumber == null || registrationNumber.isBlank()) {
            return null;
        }
        return registrationNumber.trim().toUpperCase(Locale.ROOT);
    }
}

