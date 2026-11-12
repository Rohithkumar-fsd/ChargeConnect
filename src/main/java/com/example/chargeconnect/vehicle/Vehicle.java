package com.example.chargeconnect.vehicle;

import com.example.chargeconnect.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "manufacturer", nullable = false, length = 100)
    private String manufacturer;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "registration_number", unique = true, length = 30)
    private String registrationNumber;

    @Column(name = "battery_capacity_kwh", nullable = false, precision = 8, scale = 2)
    private BigDecimal batteryCapacityKwh;

    @Column(name = "max_ac_power_kw", precision = 6, scale = 2)
    private BigDecimal maxAcPowerKw;

    @Column(name = "max_dc_power_kw", precision = 6, scale = 2)
    private BigDecimal maxDcPowerKw;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<VehicleConnector> connectors = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Vehicle() {}

    public Vehicle(
            User user,
            String manufacturer,
            String model,
            String registrationNumber,
            BigDecimal batteryCapacityKwh,
            BigDecimal maxAcPowerKw,
            BigDecimal maxDcPowerKw
    ) {
        this.user = user;
        this.manufacturer = manufacturer;
        this.model = model;
        this.registrationNumber = registrationNumber;
        this.batteryCapacityKwh = batteryCapacityKwh;
        this.maxAcPowerKw = maxAcPowerKw;
        this.maxDcPowerKw = maxDcPowerKw;
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
     * avoiding uq_vehicle_connector collisions during JPA flush ordering.
     */
    public void replaceConnectors(Set<ConnectorType> targetTypes) {
        this.connectors.removeIf(existing -> !targetTypes.contains(existing.getConnectorType()));

        Set<ConnectorType> existingTypes = this.connectors.stream()
                .map(VehicleConnector::getConnectorType)
                .collect(Collectors.toSet());

        for (ConnectorType type : targetTypes) {
            if (!existingTypes.contains(type)) {
                this.connectors.add(new VehicleConnector(this, type));
            }
        }
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getManufacturer() { return manufacturer; }
    public String getModel() { return model; }
    public String getRegistrationNumber() { return registrationNumber; }
    public BigDecimal getBatteryCapacityKwh() { return batteryCapacityKwh; }
    public BigDecimal getMaxAcPowerKw() { return maxAcPowerKw; }
    public BigDecimal getMaxDcPowerKw() { return maxDcPowerKw; }
    public Set<VehicleConnector> getConnectors() { return connectors; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public void setModel(String model) { this.model = model; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public void setBatteryCapacityKwh(BigDecimal batteryCapacityKwh) { this.batteryCapacityKwh = batteryCapacityKwh; }
    public void setMaxAcPowerKw(BigDecimal maxAcPowerKw) { this.maxAcPowerKw = maxAcPowerKw; }
    public void setMaxDcPowerKw(BigDecimal maxDcPowerKw) { this.maxDcPowerKw = maxDcPowerKw; }
}
