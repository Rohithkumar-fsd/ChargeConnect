package com.example.chargeconnect.payout.model;

import com.example.chargeconnect.user.model.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "host_payouts")
public class HostPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payout_reference", nullable = false, unique = true, length = 64)
    private String payoutReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_method", nullable = false, length = 30)
    private PayoutMethod payoutMethod;

    @Column(name = "account_details", nullable = false, length = 255)
    private String accountDetails;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PayoutStatus status = PayoutStatus.PENDING;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HostPayout() {}

    public HostPayout(
            User host,
            BigDecimal amount,
            PayoutMethod payoutMethod,
            String accountDetails
    ) {
        this.payoutReference = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        this.host = host;
        this.amount = amount;
        this.payoutMethod = payoutMethod;
        this.accountDetails = accountDetails;
        this.status = PayoutStatus.PENDING;
        this.currency = "INR";
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (payoutReference == null) {
            payoutReference = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void process(String transactionReference) {
        if (this.status != PayoutStatus.PENDING) {
            throw new IllegalStateException("Only PENDING payouts can be processed");
        }
        this.status = PayoutStatus.PROCESSED;
        this.transactionReference = transactionReference;
        this.processedAt = Instant.now();
    }

    public void reject(String reason) {
        if (this.status != PayoutStatus.PENDING) {
            throw new IllegalStateException("Only PENDING payouts can be rejected");
        }
        this.status = PayoutStatus.REJECTED;
        this.failureReason = reason;
        this.processedAt = Instant.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getPayoutReference() { return payoutReference; }
    public User getHost() { return host; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PayoutMethod getPayoutMethod() { return payoutMethod; }
    public String getAccountDetails() { return accountDetails; }
    public PayoutStatus getStatus() { return status; }
    public String getTransactionReference() { return transactionReference; }
    public Instant getProcessedAt() { return processedAt; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
