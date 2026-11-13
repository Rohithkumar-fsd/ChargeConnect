package com.example.chargeconnect.discovery.service;

import com.example.chargeconnect.discovery.dto.VehicleMatchedChargerResponse;
import com.example.chargeconnect.discovery.repo.DiscoveredChargerProjection;
import com.example.chargeconnect.discovery.repo.DiscoveryRepository;
import com.example.chargeconnect.exception.ResourceNotFoundException;
import com.example.chargeconnect.user.model.Role;
import com.example.chargeconnect.user.model.User;
import com.example.chargeconnect.user.repo.UserRepository;
import com.example.chargeconnect.vehicle.model.ConnectorType;
import com.example.chargeconnect.vehicle.model.Vehicle;
import com.example.chargeconnect.vehicle.repo.VehicleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryServiceTest {

    @Mock
    private DiscoveryRepository discoveryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private DiscoveryService discoveryService;

    private User driver;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        driver = new User("Asha Rao", "asha@example.com", "9999999999", "hashed");
        driver.addRole(Role.DRIVER);
        ReflectionTestUtils.setField(driver, "id", 10L);

        vehicle = new Vehicle(
                driver, "Tata", "Nexon EV", "KA01AB1234",
                new BigDecimal("40.50"), new BigDecimal("7.20"), new BigDecimal("50.00")
        );
        ReflectionTestUtils.setField(vehicle, "id", 101L);
        vehicle.replaceConnectors(Set.of(ConnectorType.CCS2, ConnectorType.TYPE_2));
    }

    @Test
    void findCompatibleChargersForVehicleMatchesConnectorsAndCalculatesEffectivePower() {
        DiscoveredChargerProjection p = mockProjection(
                1L, "Fast DC Hub", "COMMERCIAL", 12.9716, 77.5946,
                "DC", new BigDecimal("60.00"), new BigDecimal("24.00"), 1500.0, "CCS2,CHADEMO"
        );

        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdAndUserId(101L, 10L)).thenReturn(Optional.of(vehicle));
        when(discoveryRepository.findNearbyApprovedMatchingConnectors(eq(12.9716), eq(77.5946), eq(10000.0), anyCollection()))
                .thenReturn(List.of(p));

        List<VehicleMatchedChargerResponse> result = discoveryService.findCompatibleChargersForVehicle(
                "asha@example.com", 101L, 12.9716, 77.5946, 10000.0, null
        );

        assertEquals(1, result.size());
        VehicleMatchedChargerResponse matched = result.get(0);
        assertEquals(101L, matched.vehicleId());
        // Only CCS2 is shared between vehicle (CCS2, TYPE_2) and charger (CCS2, CHADEMO)
        assertEquals(List.of(ConnectorType.CCS2), matched.matchedConnectors());
        // Charger is DC 60 kW, but vehicle max DC is 50 kW -> effective power is 50 kW
        assertEquals(new BigDecimal("50.00"), matched.effectivePowerKw());
        assertEquals(1500.0, matched.charger().distanceMeters());
    }

    @Test
    void findCompatibleChargersFiltersByMaxPrice() {
        DiscoveredChargerProjection p1 = mockProjection(
                1L, "Cheap Hub", "COMMERCIAL", 12.9716, 77.5946,
                "AC", new BigDecimal("7.40"), new BigDecimal("15.00"), 1000.0, "TYPE_2"
        );
        DiscoveredChargerProjection p2 = mockProjection(
                2L, "Costly Hub", "COMMERCIAL", 12.9716, 77.5946,
                "AC", new BigDecimal("7.40"), new BigDecimal("30.00"), 1200.0, "TYPE_2"
        );

        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdAndUserId(101L, 10L)).thenReturn(Optional.of(vehicle));
        when(discoveryRepository.findNearbyApprovedMatchingConnectors(eq(12.9716), eq(77.5946), eq(5000.0), anyCollection()))
                .thenReturn(List.of(p1, p2));

        List<VehicleMatchedChargerResponse> result = discoveryService.findCompatibleChargersForVehicle(
                "asha@example.com", 101L, 12.9716, 77.5946, null, new BigDecimal("20.00")
        );

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).charger().id());
    }

    @Test
    void findCompatibleChargersForVehicleThrowsNotFoundWhenVehicleNotOwned() {
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdAndUserId(999L, 10L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> discoveryService.findCompatibleChargersForVehicle("asha@example.com", 999L, 12.9716, 77.5946, null, null)
        );
    }

    @Test
    void findCompatibleChargersRejectsInvalidCoordinates() {
        assertThrows(
                IllegalArgumentException.class,
                () -> discoveryService.findCompatibleChargersForVehicle("asha@example.com", 101L, 120.0, 77.5946, null, null)
        );
    }

    private DiscoveredChargerProjection mockProjection(
            Long id, String title, String locationType, double lat, double lon,
            String powerType, BigDecimal maxPower, BigDecimal price, Double dist, String connectors
    ) {
        DiscoveredChargerProjection p = mock(DiscoveredChargerProjection.class);
        when(p.getId()).thenReturn(id);
        when(p.getHostId()).thenReturn(20L);
        when(p.getHostName()).thenReturn("Host Person");
        when(p.getTitle()).thenReturn(title);
        when(p.getDescription()).thenReturn("Description");
        when(p.getLocationType()).thenReturn(locationType);
        when(p.getAddressLine()).thenReturn("12th Main");
        when(p.getCity()).thenReturn("Bengaluru");
        when(p.getState()).thenReturn("Karnataka");
        when(p.getPostalCode()).thenReturn("560038");
        when(p.getCountry()).thenReturn("India");
        when(p.getLatitude()).thenReturn(BigDecimal.valueOf(lat));
        when(p.getLongitude()).thenReturn(BigDecimal.valueOf(lon));
        when(p.getPowerType()).thenReturn(powerType);
        when(p.getMaxPowerKw()).thenReturn(maxPower);
        when(p.getPricePerKwh()).thenReturn(price);
        when(p.getAvailabilityStatus()).thenReturn("AVAILABLE");
        when(p.getDistanceMeters()).thenReturn(dist);
        when(p.getConnectorTypesCsv()).thenReturn(connectors);
        return p;
    }
}
