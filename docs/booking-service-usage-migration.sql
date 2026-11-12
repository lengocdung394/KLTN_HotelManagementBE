CREATE TABLE IF NOT EXISTS booking_service_usages (
    booking_service_usage_id VARCHAR(36) PRIMARY KEY,
    booking_service_id VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DOUBLE PRECISION NOT NULL,
    confirmed_at TIMESTAMP NOT NULL,
    confirmed_by VARCHAR(255),
    CONSTRAINT fk_booking_service_usages_service
        FOREIGN KEY (booking_service_id)
        REFERENCES booking_services (booking_service_id)
);

CREATE INDEX IF NOT EXISTS idx_booking_service_usages_confirmed_at
    ON booking_service_usages (confirmed_at);
