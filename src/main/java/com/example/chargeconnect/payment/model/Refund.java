package com.example.chargeconnect.payment.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "razorpay_refund_id", unique = true, length = 100)
    private String razorpayRefundId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String status = "PROCESSED";

    @Column(length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Refund() {}

    public Refund(Payment payment, String razorpayRefundId, BigDecimal amount, String reason) {
        this.payment = payment;
        this.razorpayRefundId = razorpayRefundId;
        this.amount = amount;
        this.reason = reason;
        this.status = "PROCESSED";
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // Getters
    public Long getId() { return id; }
    public Payment getPayment() { return payment; }
    public String getRazorpayRefundId() { return razorpayRefundId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}