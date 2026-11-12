package com.example.chargeconnect.charger.service;

import com.example.chargeconnect.charger.dto.ChargerPatchRequest;
import com.example.chargeconnect.charger.dto.ChargerRequest;
import com.example.chargeconnect.charger.dto.ChargerResponse;
import com.example.chargeconnect.charger.dto.UpdateAvailabilityRequest;
import com.example.chargeconnect.charger.dto.VerificationStatus;
import com.example.chargeconnect.charger.dto.VerifyChargerRequest;
import com.example.chargeconnect.charger.model.Charger;
import com.example.chargeconnect.charger.model.ChargerConnector;
import com.example.chargeconnect.charger.repo.ChargerRepository;
import com.example.chargeconnect.exception.ResourceNotFoundException;
import com.example.chargeconnect.user.model.User;
import com.example.chargeconnect.user.repo.UserRepository;
import com.example.chargeconnect.vehicle.model.ConnectorType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChargerService {

    private final ChargerRepository chargerRepository;
    private final UserRepository userRepository;

    public ChargerService(ChargerRepository chargerRepository, UserRepository userRepository) {
        this.chargerRepository = chargerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ChargerResponse createCharger(String hostEmail, ChargerRequest request) {
        User host = getUserByEmail(hostEmail);

        Charger charger = new Charger(
                host,
                request.title().trim(),
                normalizeOptionalText(request.description()),
                request.locationType(),
                request.addressLine().trim(),
                request.city().trim(),
                request.state().trim(),
                request.postalCode().trim(),
                request.country().trim(),
                request.latitude(),
                request.longitude(),
                request.powerType(),
                request.maxPowerKw(),
                request.pricePerKwh()
        );
        charger.replaceConnectors(request.connectorTypes());

        Charger saved = chargerRepository.saveAndFlush(charger);
        return ChargerResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ChargerResponse> getMyChargers(String hostEmail) {
        User host = getUserByEmail(hostEmail);
        return chargerRepository.findAllByHostIdOrderByCreatedAtDesc(host.getId())
                .stream()
                .map(ChargerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChargerResponse getMyCharger(String hostEmail, Long chargerId) {
        User host = getUserByEmail(hostEmail);
        Charger charger = getOwnedCharger(chargerId, host.getId());
        return ChargerResponse.from(charger);
    }

    @Transactional
    public ChargerResponse updateCharger(String hostEmail, Long chargerId, ChargerPatchRequest request) {
        User host = getUserByEmail(hostEmail);
        Charger charger = getOwnedCharger(chargerId, host.getId());

        boolean requiresReverification = false;

        if (request.title() != null) {
            charger.setTitle(requireNonBlank(request.title(), "Title"));
        }
        if (request.description() != null) {
            charger.setDescription(normalizeOptionalText(request.description()));
        }
        if (request.locationType() != null && request.locationType() != charger.getLocationType()) {
            charger.setLocationType(request.locationType());
            requiresReverification = true;
        }
        if (request.addressLine() != null) {
            String value = requireNonBlank(request.addressLine(), "Address line");
            if (!Objects.equals(value, charger.getAddressLine())) {
                charger.setAddressLine(value);
                requiresReverification = true;
            }
        }
        if (request.city() != null) {
            String value = requireNonBlank(request.city(), "City");
            if (!Objects.equals(value, charger.getCity())) {
                charger.setCity(value);
                requiresReverification = true;
            }
        }
        if (request.state() != null) {
            String value = requireNonBlank(request.state(), "State");
            if (!Objects.equals(value, charger.getState())) {
                charger.setState(value);
                requiresReverification = true;
            }
        }
        if (request.postalCode() != null) {
            String value = requireNonBlank(request.postalCode(), "Postal code");
            if (!Objects.equals(value, charger.getPostalCode())) {
                charger.setPostalCode(value);
                requiresReverification = true;
            }
        }
        if (request.country() != null) {
            String value = requireNonBlank(request.country(), "Country");
            if (!Objects.equals(value, charger.getCountry())) {
                charger.setCountry(value);
                requiresReverification = true;
            }
        }
        if (request.latitude() != null && !decimalsEqual(request.latitude(), charger.getLatitude())) {
            charger.setLatitude(request.latitude());
            requiresReverification = true;
        }
        if (request.longitude() != null && !decimalsEqual(request.longitude(), charger.getLongitude())) {
            charger.setLongitude(request.longitude());
            requiresReverification = true;
        }
        if (request.powerType() != null && request.powerType() != charger.getPowerType()) {
            charger.setPowerType(request.powerType());
            requiresReverification = true;
        }
        if (request.maxPowerKw() != null && !decimalsEqual(request.maxPowerKw(), charger.getMaxPowerKw())) {
            charger.setMaxPowerKw(request.maxPowerKw());
            requiresReverification = true;
        }
        if (request.pricePerKwh() != null) {
            charger.setPricePerKwh(request.pricePerKwh());
        }
        if (request.connectorTypes() != null) {
            if (request.connectorTypes().isEmpty()) {
                throw new IllegalArgumentException("At least one connector type is required");
            }
            Set<ConnectorType> existingTypes = charger.getConnectors().stream()
                    .map(ChargerConnector::getConnectorType)
                    .collect(Collectors.toSet());
            if (!existingTypes.equals(request.connectorTypes())) {
                charger.replaceConnectors(request.connectorTypes());
                requiresReverification = true;
            }
        }

        // Changing location or hardware specs invalidates prior verification.
        if (requiresReverification && charger.getVerificationStatus() != VerificationStatus.PENDING) {
            charger.markPendingVerification();
        }

        Charger updated = chargerRepository.saveAndFlush(charger);
        return ChargerResponse.from(updated);
    }

    @Transactional
    public ChargerResponse updateAvailability(
            String hostEmail,
            Long chargerId,
            UpdateAvailabilityRequest request
    ) {
        User host = getUserByEmail(hostEmail);
        Charger charger = getOwnedCharger(chargerId, host.getId());
        charger.setAvailabilityStatus(request.availabilityStatus());

        Charger updated = chargerRepository.saveAndFlush(charger);
        return ChargerResponse.from(updated);
    }

    @Transactional
    public void deleteCharger(String hostEmail, Long chargerId) {
        User host = getUserByEmail(hostEmail);
        Charger charger = getOwnedCharger(chargerId, host.getId());
        chargerRepository.delete(charger);
    }

    @Transactional(readOnly = true)
    public List<ChargerResponse> getChargersForAdmin(VerificationStatus status) {
        List<Charger> chargers = status == null
                ? chargerRepository.findAllByOrderByCreatedAtDesc()
                : chargerRepository.findAllByVerificationStatusOrderByCreatedAtDesc(status);

        return chargers.stream()
                .map(ChargerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChargerResponse getChargerForAdmin(Long chargerId) {
        Charger charger = chargerRepository.findWithDetailsById(chargerId)
                .orElseThrow(() -> new ResourceNotFoundException("Charger not found"));
        return ChargerResponse.from(charger);
    }

    @Transactional
    public ChargerResponse verifyCharger(
            String adminEmail,
            Long chargerId,
            VerifyChargerRequest request
    ) {
        User admin = getUserByEmail(adminEmail);
        Charger charger = chargerRepository.findWithDetailsById(chargerId)
                .orElseThrow(() -> new ResourceNotFoundException("Charger not found"));

        Instant now = Instant.now();
        if (request.decision() == VerifyChargerRequest.Decision.APPROVE) {
            charger.approve(admin, now);
        } else {
            String reason = normalizeOptionalText(request.rejectionReason());
            if (reason == null) {
                throw new IllegalArgumentException("Rejection reason is required when rejecting a charger");
            }
            charger.reject(admin, reason, now);
        }

        Charger saved = chargerRepository.saveAndFlush(charger);
        return ChargerResponse.from(saved);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private Charger getOwnedCharger(Long chargerId, Long hostId) {
        return chargerRepository.findByIdAndHostId(chargerId, hostId)
                .orElseThrow(() -> new ResourceNotFoundException("Charger not found"));
    }

    private String requireNonBlank(String value, String fieldName) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return trimmed;
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean decimalsEqual(BigDecimal first, BigDecimal second) {
        if (first == null && second == null) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        return first.compareTo(second) == 0;
    }
}

