package com.example.chargeconnect.vehicle.service;

import com.example.chargeconnect.exception.DuplicateResourceException;
import com.example.chargeconnect.exception.ResourceNotFoundException;
import com.example.chargeconnect.user.model.User;
import com.example.chargeconnect.user.repo.UserRepository;
import com.example.chargeconnect.vehicle.dto.VehiclePatchRequest;
import com.example.chargeconnect.vehicle.dto.VehicleRequest;
import com.example.chargeconnect.vehicle.dto.VehicleResponse;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private User driver;

    @BeforeEach
    void setUp() {
        driver = new User("Asha Rao", "asha@example.com", "9999999999", "hashed");
        ReflectionTestUtils.setField(driver, "id", 10L);
    }

    @Test
    void createVehicleNormalizesFieldsAndSavesConnectors() {
        VehicleRequest request = new VehicleRequest(
                "  Tata ",
                " Nexon EV ",
                " ka01ab1234 ",
                new BigDecimal("40.50"),
                new BigDecimal("7.20"),
                new BigDecimal("50.00"),
                Set.of(ConnectorType.CCS2, ConnectorType.TYPE_2)
        );

        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(driver));
        when(vehicleRepository.existsByRegistrationNumber("KA01AB1234")).thenReturn(false);
        when(vehicleRepository.saveAndFlush(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle v = invocation.getArgument(0);
            ReflectionTestUtils.setField(v, "id", 1L);
            ReflectionTestUtils.invokeMethod(v, "onCreate");
            return v;
        });

        VehicleResponse response = vehicleService.createVehicle("asha@example.com", request);

        assertEquals(1L, response.id());
        assertEquals("Tata", response.manufacturer());
        assertEquals("Nexon EV", response.model());
        assertEquals("KA01AB1234", response.registrationNumber());
        assertEquals(List.of(ConnectorType.CCS2, ConnectorType.TYPE_2), response.connectorTypes());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());

        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).saveAndFlush(captor.capture());
        assertEquals(driver, captor.getValue().getUser());
    }

    @Test
    void createVehicleConvertsBlankRegistrationNumberToNull() {
        VehicleRequest request = new VehicleRequest(
                "MG",
                "ZS EV",
                "   ",
                new BigDecimal("50.30"),
                null,
                null,
                Set.of(ConnectorType.CCS2)
        );

        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(driver));
        when(vehicleRepository.saveAndFlush(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle v = invocation.getArgument(0);
            ReflectionTestUtils.setField(v, "id", 2L);
            ReflectionTestUtils.invokeMethod(v, "onCreate");
            return v;
        });

        VehicleResponse response = vehicleService.createVehicle("asha@example.com", request);

        assertNull(response.registrationNumber());
        verify(vehicleRepository, never()).existsByRegistrationNumber(any());
    }

    @Test
    void createVehicleRejectsDuplicateRegistrationNumber() {
        VehicleRequest request = new VehicleRequest(
                "Tata",
                "Tiago EV",
                "ka01ab1234",
                new BigDecimal("24.00"),
                null,
                null,
                Set.of(ConnectorType.TYPE_2)
        );

        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(driver));
        when(vehicleRepository.existsByRegistrationNumber("KA01AB1234")).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> vehicleService.createVehicle("asha@example.com", request)
        );
        verify(vehicleRepository, never()).saveAndFlush(any());
    }

    @Test
    void getMyVehicleThrowsNotFoundWhenNotOwnedByUser() {
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdAndUserId(99L, 10L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> vehicleService.getMyVehicle("asha@example.com", 99L)
        );
    }

    @Test
    void updateVehicleAppliesPartialChangesAndReplacesConnectors() {
        Vehicle existing = new Vehicle(
                driver,
                "Hyundai",
                "Kona",
                "KA05CD5678",
                new BigDecimal("39.20"),
                new BigDecimal("7.20"),
                new BigDecimal("50.00")
        );
        ReflectionTestUtils.setField(existing, "id", 5L);
        existing.replaceConnectors(Set.of(ConnectorType.TYPE_2));

        VehiclePatchRequest patch = new VehiclePatchRequest(
                null,
                " Kona Electric ",
                "   ",
                null,
                new BigDecimal("11.00"),
                null,
                Set.of(ConnectorType.CCS2, ConnectorType.TYPE_2)
        );

        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdAndUserId(5L, 10L)).thenReturn(Optional.of(existing));
        when(vehicleRepository.saveAndFlush(existing)).thenAnswer(invocation -> {
            ReflectionTestUtils.invokeMethod(existing, "onUpdate");
            return existing;
        });

        VehicleResponse response = vehicleService.updateVehicle("asha@example.com", 5L, patch);

        assertEquals("Hyundai", response.manufacturer());
        assertEquals("Kona Electric", response.model());
        assertNull(response.registrationNumber());
        assertEquals(new BigDecimal("11.00"), response.maxAcPowerKw());
        assertEquals(List.of(ConnectorType.CCS2, ConnectorType.TYPE_2), response.connectorTypes());
    }

    @Test
    void updateVehicleRejectsBlankManufacturerOrEmptyConnectors() {
        Vehicle existing = new Vehicle(
                driver,
                "Hyundai",
                "Ioniq 5",
                null,
                new BigDecimal("72.60"),
                null,
                null
        );
        ReflectionTestUtils.setField(existing, "id", 6L);
        existing.replaceConnectors(Set.of(ConnectorType.CCS2));

        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdAndUserId(6L, 10L)).thenReturn(Optional.of(existing));

        VehiclePatchRequest blankManufacturer = new VehiclePatchRequest(
                "   ", null, null, null, null, null, null
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> vehicleService.updateVehicle("asha@example.com", 6L, blankManufacturer)
        );

        VehiclePatchRequest emptyConnectors = new VehiclePatchRequest(
                null, null, null, null, null, null, Set.of()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> vehicleService.updateVehicle("asha@example.com", 6L, emptyConnectors)
        );
    }

    @Test
    void deleteVehicleDeletesOwnedVehicle() {
        Vehicle existing = new Vehicle(
                driver,
                "BYD",
                "Atto 3",
                null,
                new BigDecimal("60.48"),
                null,
                null
        );
        ReflectionTestUtils.setField(existing, "id", 7L);

        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByIdAndUserId(7L, 10L)).thenReturn(Optional.of(existing));

        vehicleService.deleteVehicle("asha@example.com", 7L);

        verify(vehicleRepository).delete(existing);
    }
}

