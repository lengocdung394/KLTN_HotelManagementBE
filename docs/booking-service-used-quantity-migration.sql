ALTER TABLE booking_services
    ADD COLUMN IF NOT EXISTS used_quantity INTEGER;

UPDATE booking_services
SET used_quantity = quantity
WHERE is_used = TRUE
  AND (used_quantity IS NULL OR used_quantity = 0);

UPDATE booking_services
SET used_quantity = 0
WHERE used_quantity IS NULL;

ALTER TABLE booking_services
    ALTER COLUMN used_quantity SET DEFAULT 0,
    ALTER COLUMN used_quantity SET NOT NULL;
