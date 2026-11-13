-- Run this migration before deploying the application version that stores
-- the province name directly on hotels.
BEGIN;

ALTER TABLE hotels
    ADD COLUMN IF NOT EXISTS province_name VARCHAR(255);

UPDATE hotels AS hotel
SET province_name = province.name
FROM provinces AS province
WHERE hotel.province_id::text = province.province_id::text
  AND NULLIF(BTRIM(hotel.province_name), '') IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM hotels AS hotel
        LEFT JOIN provinces AS province
            ON hotel.province_id::text = province.province_id::text
        WHERE hotel.province_id IS NOT NULL
          AND province.province_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot migrate province data: one or more hotels reference a missing province';
    END IF;
END $$;

ALTER TABLE hotels
    DROP COLUMN IF EXISTS province_id;

DROP TABLE IF EXISTS provinces;

COMMIT;
