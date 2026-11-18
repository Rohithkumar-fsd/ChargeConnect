-- 1. Add rating aggregations to chargers table for fast discovery lookups
ALTER TABLE chargers ADD COLUMN IF NOT EXISTS average_rating DECIMAL(3,2) NOT NULL DEFAULT 0.00;
ALTER TABLE chargers ADD COLUMN IF NOT EXISTS total_reviews INTEGER NOT NULL DEFAULT 0;

-- 2. Create reviews table
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL UNIQUE REFERENCES charging_sessions(id) ON DELETE CASCADE,
    booking_id BIGINT NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    charger_id BIGINT NOT NULL REFERENCES chargers(id) ON DELETE CASCADE,
    driver_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    rating INTEGER NOT NULL,
    comment VARCHAR(1000),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_review_rating
        CHECK (rating >= 1 AND rating <= 5)
);

CREATE INDEX idx_reviews_charger_id ON reviews(charger_id);
CREATE INDEX idx_reviews_driver_id ON reviews(driver_id);
CREATE INDEX idx_reviews_created_at ON reviews(created_at DESC);
