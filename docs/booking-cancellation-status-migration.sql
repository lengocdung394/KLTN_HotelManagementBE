BEGIN;

DO $migration$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'booking_cancellations'
          AND column_name = 'cancellation_source'
    ) THEN
        EXECUTE $sql$
            UPDATE bookings AS booking
            SET booking_status = CASE
                WHEN UPPER(COALESCE(cancellation.cancellation_source, '')) = 'PAYMENT_TIMEOUT'
                     OR LOWER(COALESCE(cancellation.reason, '')) LIKE '%quá thời gian%'
                     OR LOWER(COALESCE(cancellation.reason, '')) LIKE '%timeout%'
                    THEN 'CANCELLED_PAYMENT_TIMEOUT'
                WHEN UPPER(COALESCE(cancellation.cancellation_source, '')) = 'STAFF'
                    THEN 'CANCELLED_BY_STAFF'
                WHEN UPPER(COALESCE(cancellation.cancellation_source, '')) = 'CUSTOMER'
                     OR cancellation.requested_by_customer_id IS NOT NULL
                    THEN 'CANCELLED_BY_CUSTOMER'
                WHEN cancellation.request_by_account_id IS NOT NULL
                    THEN 'CANCELLED_BY_STAFF'
                ELSE 'CANCELLED'
            END
            FROM booking_cancellations AS cancellation
            WHERE cancellation.booking_id = booking.booking_id
              AND booking.booking_status = 'CANCELLED'
        $sql$;

        ALTER TABLE booking_cancellations
            DROP COLUMN cancellation_source;
    ELSE
        UPDATE bookings AS booking
        SET booking_status = CASE
            WHEN LOWER(COALESCE(cancellation.reason, '')) LIKE '%quá thời gian%'
                 OR LOWER(COALESCE(cancellation.reason, '')) LIKE '%timeout%'
                THEN 'CANCELLED_PAYMENT_TIMEOUT'
            WHEN cancellation.requested_by_customer_id IS NOT NULL
                THEN 'CANCELLED_BY_CUSTOMER'
            WHEN cancellation.request_by_account_id IS NOT NULL
                THEN 'CANCELLED_BY_STAFF'
            ELSE 'CANCELLED'
        END
        FROM booking_cancellations AS cancellation
        WHERE cancellation.booking_id = booking.booking_id
          AND booking.booking_status = 'CANCELLED';
    END IF;
END
$migration$;

COMMIT;
