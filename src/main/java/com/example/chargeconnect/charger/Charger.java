package com.example.chargeconnect.charger;

import com.example.chargeconnect.user.User;
import com.example.chargeconnect.vehicle.ConnectorType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "chargers")
public class Charger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 30)
    private LocationType locationType;

    @Column(name = "address_line", nullable = false, length = 255)
    private String addressLine;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(nullable = false, length = 80)
    private String country;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "power_type", nullable = false, length = 10)
    private PowerType powerType;

    @Column(name = "max_power_kw", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxPowerKw;

    @Column(name = "price_per_kwh", nullable = false, precision = 8, scale = 2)
    private BigDecimal pricePerKwh;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 20)
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @OneToMany(mappedBy = "charger", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ChargerConnector> connectors = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Charger() {}

    public Charger(
            User host,
            String title,
            String description,
            LocationType locationType,
            String addressLine,
            String city,
            String state,
            String postalCode,
            String country,
            BigDecimal latitude,
            BigDecimal longitude,
            PowerType powerType,
            BigDecimal maxPowerKw,
            BigDecimal pricePerKwh
    ) {
        this.host = host;
        this.title = title;
        this.description = description;
        this.locationType = locationType;
        this.addressLine = addressLine;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.powerType = powerType;
        this.maxPowerKw = maxPowerKw;
        this.pricePerKwh = pricePerKwh;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Synchronizes child connector rows without dropping retained types,
     * avoiding uq_charger_connector collisions during JPA flush ordering.
     */
    public void replaceConnectors(Set<ConnectorType> targetTypes) {
        this.connectors.removeIf(existing -> !targetTypes.contains(existing.getConnectorType()));

        Set<ConnectorType> existingTypes = this.connectors.stream()
                .map(ChargerConnector::getConnectorType)
                .collect(Collectors.toSet());

        for (ConnectorType type : targetTypes) {
            if (!existingTypes.contains(type)) {
                this.connectors.add(new ChargerConnector(this, type));
            }
        }
    }

    public void markPendingVerification() {
        this.verificationStatus = VerificationStatus.PENDING;
        this.rejectionReason = null;
        this.verifiedBy = null;
        this.verifiedAt = null;
    }

    public void approve(User admin, Instant timestamp) {
        this.verificationStatus = VerificationStatus.APPROVED;
        this.rejectionReason = null;
        this.verifiedBy = admin;
        this.verifiedAt = timestamp;
    }

    public void reject(User admin, String reason, Instant timestamp) {
        this.verificationStatus = VerificationStatus.REJECTED;
        this.rejectionReason = reason;
        this.verifiedBy = admin;
        this.verifiedAt = timestamp;
    }

    public Long getId() { return id; }
    public User getHost() { return host; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocationType getLocationType() { return locationType; }
    public String getAddressLine() { return addressLine; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public PowerType getPowerType() { return powerType; }
    public BigDecimal getMaxPowerKw() { return maxPowerKw; }
    public BigDecimal getPricePerKwh() { return pricePerKwh; }
    public AvailabilityStatus getAvailabilityStatus() { return availabilityStatus; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public String getRejectionReason() { return rejectionReason; }
    public User getVerifiedBy() { return verifiedBy; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Set<ChargerConnector> getConnectors() { return connectors; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setLocationType(LocationType locationType) { this.locationType = locationType; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public void setCountry(String country) { this.country = country; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public void setPowerType(PowerType powerType) { this.powerType = powerType; }
    public void setMaxPowerKw(BigDecimal maxPowerKw) { this.maxPowerKw = maxPowerKw; }
    public void setPricePerKwh(BigDecimal pricePerKwh) { this.pricePerKwh = pricePerKwh; }
    public void setAvailabilityStatus(AvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }
}

