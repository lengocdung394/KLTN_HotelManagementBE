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
        } catch (RuntimeException exception) {
            log.error("Could not synchronize bookings_booking_status_check", exception);
        }
    }
}
