-- V6__create_booking_tables.sql
-- Adds operating hours to chargers and creates the bookings table with constraints and indexes.

-- 1. Add optional operating hours to chargers (defaults to 24/7 if not specified)
ALTER TABLE chargers
    ADD COLUMN IF NOT EXISTS open_time TIME NOT NULL DEFAULT '00:00:00',
    ADD COLUMN IF NOT EXISTS close_time TIME NOT NULL DEFAULT '23:59:59';

-- 2. Create bookings table
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    booking_reference VARCHAR(36) NOT NULL UNIQUE,
    driver_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    charger_id BIGINT NOT NULL REFERENCES chargers(id) ON DELETE CASCADE,
    vehicle_id BIGINT REFERENCES vehicles(id) ON DELETE SET NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    total_hours NUMERIC(4, 2) NOT NULL,
    price_per_kwh NUMERIC(8, 2) NOT NULL,
    estimated_kwh NUMERIC(6, 2) NOT NULL,
    total_price NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    cancellation_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_booking_times CHECK (end_time > start_time),
    CONSTRAINT chk_booking_status CHECK (
        status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'EXPIRED', 'BLOCKED_BY_HOST')
    )
);

-- 3. Indexes for fast query lookup & conflict resolution
CREATE INDEX idx_bookings_driver_id ON bookings(driver_id);
CREATE INDEX idx_bookings_charger_id ON bookings(charger_id);
CREATE INDEX idx_bookings_charger_status_dates ON bookings(charger_id, status, start_time, end_time);
CREATE INDEX idx_bookings_reference ON bookings(booking_reference);

