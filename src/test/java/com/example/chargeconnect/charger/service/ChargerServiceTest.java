package com.example.chargeconnect.charger.service;

import com.example.chargeconnect.charger.AvailabilityStatus;
import com.example.chargeconnect.charger.Charger;
import com.example.chargeconnect.charger.ChargerRepository;
import com.example.chargeconnect.charger.LocationType;
import com.example.chargeconnect.charger.PowerType;
import com.example.chargeconnect.charger.VerificationStatus;
import com.example.chargeconnect.charger.dto.ChargerPatchRequest;
import com.example.chargeconnect.charger.dto.ChargerRequest;
import com.example.chargeconnect.charger.dto.ChargerResponse;
import com.example.chargeconnect.charger.dto.UpdateAvailabilityRequest;
import com.example.chargeconnect.charger.dto.VerifyChargerRequest;
import com.example.chargeconnect.exception.ResourceNotFoundException;
import com.example.chargeconnect.user.Role;
import com.example.chargeconnect.user.User;
import com.example.chargeconnect.user.UserRepository;
import com.example.chargeconnect.vehicle.ConnectorType;
import java.math.BigDecimal;
import java.time.Instant;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChargerServiceTest {

    @Mock
    private ChargerRepository chargerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChargerService chargerService;

    private User host;
    private User admin;

    @BeforeEach
    void setUp() {
        host = new User("Vikram Host", "host@example.com", "9876543210", "hashed");
        host.addRole(Role.HOST);
        ReflectionTestUtils.setField(host, "id", 20L);

        admin = new User("Admin User", "admin@example.com", null, "hashedAdmin");
        admin.addRole(Role.ADMIN);
        ReflectionTestUtils.setField(admin, "id", 1L);
    }

    @Test
    void createChargerStartsPendingAndAvailableWithNormalizedFields() {
        ChargerRequest request = new ChargerRequest(
                "  Indiranagar Home Wallbox ",
                "  Covered parking with 24x7 access  ",
                LocationType.HOME,
                " 12th Main Road ",
                " Bengaluru ",
                " Karnataka ",
                " 560038 ",
                " India ",
                new BigDecimal("12.971600"),
                new BigDecimal("77.594600"),
                PowerType.AC,
                new BigDecimal("7.40"),
                new BigDecimal("18.50"),
                Set.of(ConnectorType.TYPE_2, ConnectorType.BHARAT_AC_001)
        );

        when(userRepository.findByEmail("host@example.com")).thenReturn(Optional.of(host));
        when(chargerRepository.saveAndFlush(any(Charger.class))).thenAnswer(invocation -> {
            Charger c = invocation.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 101L);
            ReflectionTestUtils.invokeMethod(c, "onCreate");
            return c;
        });

        ChargerResponse response = chargerService.createCharger("host@example.com", request);

        assertEquals(101L, response.id());
        assertEquals(20L, response.hostId());
        assertEquals("Indiranagar Home Wallbox", response.title());
        assertEquals(AvailabilityStatus.AVAILABLE, response.availabilityStatus());
        assertEquals(VerificationStatus.PENDING, response.verificationStatus());
        assertEquals(List.of(ConnectorType.BHARAT_AC_001, ConnectorType.TYPE_2), response.connectorTypes());
    }

    @Test
    void updateChargerKeepsApprovedStatusWhenOnlyPriceOrDescriptionChanges() {
        Charger charger = buildApprovedCharger();

        when(userRepository.findByEmail("host@example.com")).thenReturn(Optional.of(host));
        when(chargerRepository.findByIdAndHostId(101L, 20L)).thenReturn(Optional.of(charger));
        when(chargerRepository.saveAndFlush(charger)).thenAnswer(invocation -> {
            ReflectionTestUtils.invokeMethod(charger, "onUpdate");
            return charger;
        });

        ChargerPatchRequest patch = new ChargerPatchRequest(
                null, "Updated description", null, null, null, null, null, null,
                null, null, null, null, new BigDecimal("21.00"), null
        );

        ChargerResponse response = chargerService.updateCharger("host@example.com", 101L, patch);

        assertEquals(VerificationStatus.APPROVED, response.verificationStatus());
        assertEquals(new BigDecimal("21.00"), response.pricePerKwh());
        assertEquals("Updated description", response.description());
    }

    @Test
    void updateChargerResetsVerificationToPendingWhenLocationOrConnectorsChange() {
        Charger charger = buildApprovedCharger();

        when(userRepository.findByEmail("host@example.com")).thenReturn(Optional.of(host));
        when(chargerRepository.findByIdAndHostId(101L, 20L)).thenReturn(Optional.of(charger));
        when(chargerRepository.saveAndFlush(charger)).thenAnswer(invocation -> {
            ReflectionTestUtils.invokeMethod(charger, "onUpdate");
            return charger;
        });

        ChargerPatchRequest patch = new ChargerPatchRequest(
                null, null, null, null, null, null, null, null,
                new BigDecimal("12.980000"), null, null, null, null, null
        );

        ChargerResponse response = chargerService.updateCharger("host@example.com", 101L, patch);

        assertEquals(VerificationStatus.PENDING, response.verificationStatus());
        assertNull(response.verifiedByUserId());
        assertNull(response.verifiedAt());
    }

    @Test
    void updateAvailabilityChangesStatusForOwnedCharger() {
        Charger charger = buildApprovedCharger();

        when(userRepository.findByEmail("host@example.com")).thenReturn(Optional.of(host));
        when(chargerRepository.findByIdAndHostId(101L, 20L)).thenReturn(Optional.of(charger));
        when(chargerRepository.saveAndFlush(charger)).thenReturn(charger);

        ChargerResponse response = chargerService.updateAvailability(
                "host@example.com",
                101L,
                new UpdateAvailabilityRequest(AvailabilityStatus.MAINTENANCE)
        );

        assertEquals(AvailabilityStatus.MAINTENANCE, response.availabilityStatus());
    }

    @Test
    void verifyChargerApprovesAndRejectsWithValidation() {
        Charger charger = buildApprovedCharger();
        charger.markPendingVerification();

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(chargerRepository.findWithDetailsById(101L)).thenReturn(Optional.of(charger));
        when(chargerRepository.saveAndFlush(charger)).thenReturn(charger);

        ChargerResponse approved = chargerService.verifyCharger(
                "admin@example.com",
                101L,
                new VerifyChargerRequest(VerifyChargerRequest.Decision.APPROVE, null)
        );
        assertEquals(VerificationStatus.APPROVED, approved.verificationStatus());
        assertEquals(1L, approved.verifiedByUserId());

        assertThrows(
                IllegalArgumentException.class,
                () -> chargerService.verifyCharger(
                        "admin@example.com",
                        101L,
                        new VerifyChargerRequest(VerifyChargerRequest.Decision.REJECT, "   ")
                )
        );

        ChargerResponse rejected = chargerService.verifyCharger(
                "admin@example.com",
                101L,
                new VerifyChargerRequest(VerifyChargerRequest.Decision.REJECT, "Invalid address proof")
        );
        assertEquals(VerificationStatus.REJECTED, rejected.verificationStatus());
        assertEquals("Invalid address proof", rejected.rejectionReason());
    }

    @Test
    void getMyChargerThrowsNotFoundWhenNotOwned() {
        when(userRepository.findByEmail("host@example.com")).thenReturn(Optional.of(host));
        when(chargerRepository.findByIdAndHostId(999L, 20L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> chargerService.getMyCharger("host@example.com", 999L)
        );
    }

    private Charger buildApprovedCharger() {
        Charger charger = new Charger(
                host,
                "Indiranagar Wallbox",
                "Covered",
                LocationType.HOME,
                "12th Main",
                "Bengaluru",
                "Karnataka",
                "560038",
                "India",
                new BigDecimal("12.971600"),
                new BigDecimal("77.594600"),
                PowerType.AC,
                new BigDecimal("7.40"),
                new BigDecimal("18.50")
        );
        ReflectionTestUtils.setField(charger, "id", 101L);
        charger.replaceConnectors(Set.of(ConnectorType.TYPE_2));
        charger.approve(admin, Instant.parse("2026-11-12T10:00:00Z"));
        ReflectionTestUtils.invokeMethod(charger, "onCreate");
        return charger;
    }
}

