package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate (ddl-auto=update) không cập nhật CHECK constraint cũ của cột bookings.booking_status.
 * DB tạo từ phiên bản enum cũ chỉ cho phép 0..4, nên khi scheduler gán CANCELLED_PAYMENT_TIMEOUT (ordinal 5)
 * thì PostgreSQL từ chối và phòng bị kẹt ở PENDING. Component này đồng bộ constraint theo enum hiện tại.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingStatusConstraintMigration {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void syncBookingStatusConstraint() {
        int maxOrdinal = BookingStatus.values().length - 1;
        try {
            jdbcTemplate.execute("ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_booking_status_check");
            jdbcTemplate.execute("ALTER TABLE bookings ADD CONSTRAINT bookings_booking_status_check "
                    + "CHECK (booking_status >= 0 AND booking_status <= " + maxOrdinal + ")");
            log.info("Synchronized bookings_booking_status_check to 0..{}", maxOrdinal);

            jdbcTemplate.execute("ALTER TABLE payment_transactions DROP CONSTRAINT IF EXISTS payment_transactions_payment_source_check");
            jdbcTemplate.execute("ALTER TABLE payment_transactions ADD CONSTRAINT payment_transactions_payment_source_check "
                    + "CHECK (payment_source IN ('FRONT_DESK', 'CUSTOMER', 'ONLINE_GATEWAY', 'ONLINE_BOOKING', 'UNKNOWN'))");
            log.info("Synchronized payment_transactions_payment_source_check constraint");

            jdbcTemplate.execute("ALTER TABLE booking_cancellations DROP CONSTRAINT IF EXISTS booking_cancellations_status_check");
            jdbcTemplate.execute("ALTER TABLE booking_cancellations ADD CONSTRAINT booking_cancellations_status_check "
                    + "CHECK (status IN ('REFUND_BANK_DETAILS_REQUIRED', 'REFUND_REVIEW_PENDING', 'REFUND_APPROVED', 'REFUND_REJECTED', 'PAYOUT_PROCESSING', 'PAYOUT_FAILED', 'REFUNDED', 'NO_REFUND_REQUIRED'))");
            log.info("Synchronized booking_cancellations_status_check constraint");

            jdbcTemplate.execute("ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check");
            jdbcTemplate.execute("ALTER TABLE notifications ADD CONSTRAINT notifications_type_check "
                    + "CHECK (type IN ('BOOKING_CREATED', 'BOOKING_UPDATED', 'BOOKING_CANCELLED', 'ROOM_CREATED', 'ROOM_UPDATED', 'BUILDING_CREATED', 'BUILDING_UPDATED', 'FLOOR_CREATED', 'FLOOR_UPDATED', 'ROOM_POLICY_CREATED', 'ROOM_POLICY_UPDATED', 'EMPLOYEE_CREATED', 'EMPLOYEE_UPDATED', 'PROMOTION_CREATED', 'PROMOTION_UPDATED', 'SERVICE_CREATED', 'SERVICE_UPDATED', 'SERVICE_IMPORT_COMPLETED', 'SERVICE_IMPORT_FAILED', 'SYSTEM', 'PAYMENT_RECEIVED', 'BOOKING_REFUND_COMPLETED', 'BOOKING_REFUND_REQUESTED', 'SHIFT_SCHEDULE_REMINDER', 'HOUSEKEEPING_TASK_ASSIGNED', 'HOUSEKEEPING_TASK_REASSIGNED', 'HOUSEKEEPING_TASK_COMPLETED'))");
            log.info("Synchronized notifications_type_check constraint");
        } catch (RuntimeException exception) {
            log.error("Could not synchronize database constraints", exception);
        }
    }
}
