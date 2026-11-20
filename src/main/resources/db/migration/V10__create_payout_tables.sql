-- V10: Create Host Payouts Table for Marketplace Settlement

CREATE TABLE host_payouts (
    id BIGSERIAL PRIMARY KEY,
    payout_reference VARCHAR(64) NOT NULL UNIQUE,
    host_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    payout_method VARCHAR(30) NOT NULL,
    account_details VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    transaction_reference VARCHAR(100),
    processed_at TIMESTAMPTZ,
    failure_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_host_payout_method CHECK (payout_method IN ('UPI', 'BANK_TRANSFER')),
    CONSTRAINT chk_host_payout_status CHECK (status IN ('PENDING', 'PROCESSED', 'REJECTED'))
);

CREATE INDEX idx_host_payouts_host_id ON host_payouts(host_id);
CREATE INDEX idx_host_payouts_status ON host_payouts(status);
