-- 1. Add unique QR code identifier to chargers for physical check-in
ALTER TABLE chargers ADD COLUMN IF NOT EXISTS qr_code VARCHAR(64) UNIQUE;

-- Backfill existing chargers with default QR codes: CHG-QR-<id>
UPDATE chargers SET qr_code = 'CHG-QR-' || id WHERE qr_code IS NULL;

-- Make qr_code NOT NULL now that existing rows are populated
ALTER TABLE chargers ALTER COLUMN qr_code SET NOT NULL;

-- 2. Create charging_sessions table
CREATE TABLE charging_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_reference VARCHAR(64) NOT NULL UNIQUE,
    booking_id BIGINT NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    driver_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    charger_id BIGINT NOT NULL REFERENCES chargers(id) ON DELETE CASCADE,
    vehicle_id BIGINT REFERENCES vehicles(id) ON DELETE SET NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    start_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMPTZ,

    initial_meter_kwh DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    final_meter_kwh DECIMAL(10,2),
    energy_delivered_kwh DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    price_per_kwh DECIMAL(8,2) NOT NULL,
    total_cost DECIMAL(10,2),
    stop_reason VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_session_status
        CHECK (status IN ('ACTIVE', 'COMPLETED', 'STOPPED_EARLY', 'EMERGENCY_STOPPED')),

    CONSTRAINT chk_meter_readings
        CHECK (final_meter_kwh IS NULL OR final_meter_kwh >= initial_meter_kwh),

    CONSTRAINT chk_energy_positive
        CHECK (energy_delivered_kwh >= 0)
);

CREATE INDEX idx_sessions_booking_id ON charging_sessions(booking_id);
CREATE INDEX idx_sessions_driver_id ON charging_sessions(driver_id);
CREATE INDEX idx_sessions_charger_id ON charging_sessions(charger_id);
CREATE INDEX idx_sessions_status ON charging_sessions(status);