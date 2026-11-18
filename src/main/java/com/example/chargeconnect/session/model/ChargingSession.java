package com.example.chargeconnect.session.model;

import com.example.chargeconnect.booking.model.Booking;
import com.example.chargeconnect.charger.model.Charger;
import com.example.chargeconnect.user.model.User;
import com.example.chargeconnect.vehicle.model.Vehicle;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "charging_sessions")
public class ChargingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_reference", nullable = false, unique = true, length = 64)
    private String sessionReference;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "charger_id", nullable = false)
    private Charger charger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "initial_meter_kwh", nullable = false, precision = 10, scale = 2)
    private BigDecimal initialMeterKwh = BigDecimal.ZERO;

    @Column(name = "final_meter_kwh", precision = 10, scale = 2)
    private BigDecimal finalMeterKwh;

    @Column(name = "energy_delivered_kwh", nullable = false, precision = 10, scale = 2)
    private BigDecimal energyDeliveredKwh = BigDecimal.ZERO;

    @Column(name = "price_per_kwh", nullable = false, precision = 8, scale = 2)
    private BigDecimal pricePerKwh;

    @Column(name = "total_cost", precision = 10, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "stop_reason", length = 255)
    private String stopReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ChargingSession() {}

    public ChargingSession(
            Booking booking,
            User driver,
            Charger charger,
            Vehicle vehicle,
            BigDecimal initialMeterKwh
    ) {
        this.sessionReference = "SES-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        this.booking = booking;
        this.driver = driver;
        this.charger = charger;
        this.vehicle = vehicle;
        this.status = SessionStatus.ACTIVE;
        this.startTime = Instant.now();
        this.initialMeterKwh = initialMeterKwh != null ? initialMeterKwh : BigDecimal.ZERO;
        this.pricePerKwh = charger.getPricePerKwh();
        this.energyDeliveredKwh = BigDecimal.ZERO;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (startTime == null) startTime = now;
        if (sessionReference == null) {
            sessionReference = "SES-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Completes charging, calculates final delivered energy and total cost.
     */
    public void complete(Instant stopTime, String reason) {
        if (this.status != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE sessions can be stopped");
        }
        this.endTime = stopTime != null ? stopTime : Instant.now();
        this.status = SessionStatus.COMPLETED;
        this.stopReason = reason != null ? reason : "Normal completion";

        // Calculate energy based on elapsed time and charger max power
        this.energyDeliveredKwh = calculateDeliveredEnergy(this.endTime);
        this.totalCost = this.energyDeliveredKwh.multiply(this.pricePerKwh).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Emergency stops the session.
     */
    public void emergencyStop(String reason) {
        if (this.status != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE sessions can be emergency stopped");
        }
        this.endTime = Instant.now();
        this.status = SessionStatus.EMERGENCY_STOPPED;
        this.stopReason = reason != null ? reason : "Emergency stop invoked";
        this.energyDeliveredKwh = calculateDeliveredEnergy(this.endTime);
        this.totalCost = this.energyDeliveredKwh.multiply(this.pricePerKwh).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates energy delivered based on power rating and elapsed time, capped by vehicle battery if known.
     */
    public BigDecimal calculateDeliveredEnergy(Instant asOfTime) {
        if (asOfTime == null || asOfTime.isBefore(this.startTime)) {
            return BigDecimal.ZERO;
        }
        long seconds = Duration.between(this.startTime, asOfTime).getSeconds();
        if (seconds <= 0) {
            return BigDecimal.ZERO;
        }
        double hours = seconds / 3600.0;
        BigDecimal powerKw = charger.getMaxPowerKw();
        BigDecimal delivered = powerKw.multiply(BigDecimal.valueOf(hours)).setScale(2, RoundingMode.HALF_UP);

        // Cap at vehicle battery capacity if vehicle is associated
        if (vehicle != null && vehicle.getBatteryCapacityKwh() != null) {
            if (delivered.compareTo(vehicle.getBatteryCapacityKwh()) > 0) {
                delivered = vehicle.getBatteryCapacityKwh();
            }
        }
        return delivered;
    }

    // Getters
    public Long getId() { return id; }
    public String getSessionReference() { return sessionReference; }
    public Booking getBooking() { return booking; }
    public User getDriver() { return driver; }
    public Charger getCharger() { return charger; }
    public Vehicle getVehicle() { return vehicle; }
    public SessionStatus getStatus() { return status; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public BigDecimal getInitialMeterKwh() { return initialMeterKwh; }
    public BigDecimal getFinalMeterKwh() { return finalMeterKwh; }
    public BigDecimal getEnergyDeliveredKwh() { return energyDeliveredKwh; }
    public BigDecimal getPricePerKwh() { return pricePerKwh; }
    public BigDecimal getTotalCost() { return totalCost; }
    public String getStopReason() { return stopReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setFinalMeterKwh(BigDecimal finalMeterKwh) { this.finalMeterKwh = finalMeterKwh; }
}