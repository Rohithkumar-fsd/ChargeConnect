CREATE TABLE chargers (
    id BIGSERIAL PRIMARY KEY,
    host_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    title VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    location_type VARCHAR(30) NOT NULL,

    address_line VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(80) NOT NULL,

    latitude DECIMAL(9,6) NOT NULL,
    longitude DECIMAL(9,6) NOT NULL,
    location GEOGRAPHY(Point, 4326),

    power_type VARCHAR(10) NOT NULL,
    max_power_kw DECIMAL(6,2) NOT NULL,
    price_per_kwh DECIMAL(8,2) NOT NULL,

    availability_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500),
    verified_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    verified_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_charger_location_type
        CHECK (location_type IN ('HOME', 'HOTEL', 'RESTAURANT', 'OFFICE', 'COMMERCIAL', 'OTHER')),

    CONSTRAINT chk_charger_latitude
        CHECK (latitude >= -90 AND latitude <= 90),

    CONSTRAINT chk_charger_longitude
        CHECK (longitude >= -180 AND longitude <= 180),

    CONSTRAINT chk_charger_power_type
        CHECK (power_type IN ('AC', 'DC', 'BOTH')),

    CONSTRAINT chk_charger_max_power
        CHECK (max_power_kw > 0),

    CONSTRAINT chk_charger_price_per_kwh
        CHECK (price_per_kwh > 0),

    CONSTRAINT chk_charger_availability_status
        CHECK (availability_status IN ('AVAILABLE', 'UNAVAILABLE', 'MAINTENANCE')),

    CONSTRAINT chk_charger_verification_status
        CHECK (verification_status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE TABLE charger_connectors (
    id BIGSERIAL PRIMARY KEY,
    charger_id BIGINT NOT NULL REFERENCES chargers(id) ON DELETE CASCADE,
    connector_type VARCHAR(50) NOT NULL,

    CONSTRAINT uq_charger_connector
        UNIQUE (charger_id, connector_type)
);

CREATE INDEX idx_chargers_host_id
    ON chargers(host_id);

CREATE INDEX idx_chargers_verification_status
    ON chargers(verification_status);

CREATE INDEX idx_chargers_location
    ON chargers USING GIST (location);

CREATE OR REPLACE FUNCTION sync_charger_location()
RETURNS TRIGGER AS $$
BEGIN
    -- PostGIS expects (X = longitude, Y = latitude) in SRID 4326.
    NEW.location := ST_SetSRID(
        ST_MakePoint(NEW.longitude::double precision, NEW.latitude::double precision),
        4326
    )::geography;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_charger_location
    BEFORE INSERT OR UPDATE OF latitude, longitude ON chargers
    FOR EACH ROW
    EXECUTE FUNCTION sync_charger_location();

