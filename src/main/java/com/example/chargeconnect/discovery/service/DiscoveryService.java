package com.example.chargeconnect.discovery.service;

import com.example.chargeconnect.charger.model.PowerType;
import com.example.chargeconnect.discovery.dto.DiscoveredChargerResponse;
import com.example.chargeconnect.discovery.dto.VehicleMatchedChargerResponse;
import com.example.chargeconnect.discovery.repo.DiscoveredChargerProjection;
import com.example.chargeconnect.discovery.repo.DiscoveryRepository;
import com.example.chargeconnect.exception.ResourceNotFoundException;
import com.example.chargeconnect.user.model.User;
import com.example.chargeconnect.user.repo.UserRepository;
import com.example.chargeconnect.vehicle.model.ConnectorType;
import com.example.chargeconnect.vehicle.model.Vehicle;
import com.example.chargeconnect.vehicle.model.VehicleConnector;
import com.example.chargeconnect.vehicle.repo.VehicleRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscoveryService {

    public static final double DEFAULT_RADIUS_METERS = 5000.0;
    public static final double MAX_RADIUS_METERS = 50000.0;

    private final DiscoveryRepository discoveryRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public DiscoveryService(
            DiscoveryRepository discoveryRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository
    ) {
        this.discoveryRepository = discoveryRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public List<VehicleMatchedChargerResponse> findCompatibleChargersForVehicle(
            String driverEmail,
            Long vehicleId,
            double latitude,
            double longitude,
            Double radiusMeters,
            BigDecimal maxPricePerKwh
    ) {
        validateCoordinates(latitude, longitude);
        double effectiveRadius = sanitizeRadius(radiusMeters);

        User driver = userRepository.findByEmail(driverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated driver not found"));

        Vehicle vehicle = vehicleRepository.findByIdAndUserId(vehicleId, driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        Set<ConnectorType> vehicleConnectors = vehicle.getConnectors().stream()
                .map(VehicleConnector::getConnectorType)
                .collect(Collectors.toSet());

        if (vehicleConnectors.isEmpty()) {
            throw new IllegalArgumentException("Vehicle has no registered connector types");
        }

        Set<String> connectorNames = vehicleConnectors.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        List<DiscoveredChargerProjection> projections = discoveryRepository.findNearbyApprovedMatchingConnectors(
                latitude, longitude, effectiveRadius, connectorNames
        );

        return projections.stream()
                .map(DiscoveredChargerResponse::fromProjection)
                .filter(charger -> maxPricePerKwh == null || charger.pricePerKwh().compareTo(maxPricePerKwh) <= 0)
                .map(charger -> {
                    List<ConnectorType> matched = charger.connectorTypes().stream()
                            .filter(vehicleConnectors::contains)
                            .sorted(Comparator.comparing(Enum::name))
                            .toList();

                    BigDecimal effectivePower = calculateEffectivePower(charger, vehicle);

                    return new VehicleMatchedChargerResponse(
                            charger,
                            vehicle.getId(),
                            matched,
                            effectivePower
                    );
                })
                .toList();
    }

    private BigDecimal calculateEffectivePower(DiscoveredChargerResponse charger, Vehicle vehicle) {
        BigDecimal chargerPower = charger.maxPowerKw();
        if (charger.powerType() == PowerType.AC) {
            return vehicle.getMaxAcPowerKw() != null
                    ? chargerPower.min(vehicle.getMaxAcPowerKw())
                    : chargerPower;
        }
        if (charger.powerType() == PowerType.DC) {
            return vehicle.getMaxDcPowerKw() != null
                    ? chargerPower.min(vehicle.getMaxDcPowerKw())
                    : chargerPower;
        }
        // PowerType.BOTH: choose vehicle's highest compatible capability
        BigDecimal vehicleMax = null;
        if (vehicle.getMaxDcPowerKw() != null && vehicle.getMaxAcPowerKw() != null) {
            vehicleMax = vehicle.getMaxDcPowerKw().max(vehicle.getMaxAcPowerKw());
        } else if (vehicle.getMaxDcPowerKw() != null) {
            vehicleMax = vehicle.getMaxDcPowerKw();
        } else if (vehicle.getMaxAcPowerKw() != null) {
            vehicleMax = vehicle.getMaxAcPowerKw();
        }

        return vehicleMax != null ? chargerPower.min(vehicleMax) : chargerPower;
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    private double sanitizeRadius(Double radiusMeters) {
        if (radiusMeters == null || radiusMeters <= 0.0) {
            return DEFAULT_RADIUS_METERS;
        }
        return Math.min(radiusMeters, MAX_RADIUS_METERS);
    }
}
