
CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    manufacturer VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    registration_number VARCHAR(30) UNIQUE,

    battery_capacity_kwh DECIMAL(8,2) NOT NULL,
    max_ac_power_kw DECIMAL(6,2),
    max_dc_power_kw DECIMAL(6,2),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_battery_capacity
        CHECK (battery_capacity_kwh > 0),

    CONSTRAINT chk_ac_power
        CHECK (max_ac_power_kw IS NULL OR max_ac_power_kw > 0),

    CONSTRAINT chk_dc_power
        CHECK (max_dc_power_kw IS NULL OR max_dc_power_kw > 0)
);

CREATE TABLE vehicle_connectors (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL
        REFERENCES vehicles(id) ON DELETE CASCADE,

    connector_type VARCHAR(50) NOT NULL,

    CONSTRAINT uq_vehicle_connector
        UNIQUE (vehicle_id, connector_type)
);

CREATE INDEX idx_vehicles_user_id
ON vehicles(user_id);
