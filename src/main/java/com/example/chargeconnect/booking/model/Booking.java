package com.example.chargeconnect.booking.model;

import com.example.chargeconnect.charger.model.Charger;
import com.example.chargeconnect.user.model.User;
import com.example.chargeconnect.vehicle.model.Vehicle;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_reference", nullable = false, unique = true, length = 36)
    private String bookingReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "charger_id", nullable = false)
    private Charger charger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "total_hours", nullable = false, precision = 4, scale = 2)
    private BigDecimal totalHours;

    @Column(name = "price_per_kwh", nullable = false, precision = 8, scale = 2)
    private BigDecimal pricePerKwh;

    @Column(name = "estimated_kwh", nullable = false, precision = 6, scale = 2)
    private BigDecimal estimatedKwh;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Booking() {}

    public Booking(
            User driver,
            Charger charger,
            Vehicle vehicle,
            Instant startTime,
            Instant endTime,
            BigDecimal totalHours,
            BigDecimal pricePerKwh,
            BigDecimal estimatedKwh,
            BigDecimal totalPrice,
            BookingStatus status
    ) {
        this.bookingReference = UUID.randomUUID().toString();
        this.driver = driver;
        this.charger = charger;
        this.vehicle = vehicle;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalHours = totalHours;
        this.pricePerKwh = pricePerKwh;
        this.estimatedKwh = estimatedKwh;
        this.totalPrice = totalPrice;
        this.status = status != null ? status : BookingStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.bookingReference == null) {
            this.bookingReference = UUID.randomUUID().toString();
        }
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void confirm() {
        if (this.status != BookingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING bookings can be confirmed");
        }
        this.status = BookingStatus.CONFIRMED;
    }

    public void cancel(String reason) {
        if (this.status != BookingStatus.PENDING && this.status != BookingStatus.CONFIRMED && this.status != BookingStatus.BLOCKED_BY_HOST) {
            throw new IllegalStateException("Cannot cancel a booking with status: " + this.status);
        }
        this.status = BookingStatus.CANCELLED;
        this.cancellationReason = reason;
    }

    public void expire() {
        if (this.status == BookingStatus.PENDING) {
            this.status = BookingStatus.EXPIRED;
        }
    }

    public void complete() {
        if (this.status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED bookings can be completed");
        }
        this.status = BookingStatus.COMPLETED;
    }

    public Long getId() { return id; }
    public String getBookingReference() { return bookingReference; }
    public User getDriver() { return driver; }
    public Charger getCharger() { return charger; }
    public Vehicle getVehicle() { return vehicle; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public BigDecimal getTotalHours() { return totalHours; }
    public BigDecimal getPricePerKwh() { return pricePerKwh; }
    public BigDecimal getEstimatedKwh() { return estimatedKwh; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public BookingStatus getStatus() { return status; }
    public String getCancellationReason() { return cancellationReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
