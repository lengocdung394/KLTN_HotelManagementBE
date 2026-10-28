package iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums;

import java.util.List;

public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    CANCELLED_BY_CUSTOMER,
    CANCELLED_BY_STAFF,
    CANCELLED_PAYMENT_TIMEOUT,
    IN_HOUSE,
    COMPLETED;

    public boolean isCancelled() {
        return this == CANCELLED
                || this == CANCELLED_BY_CUSTOMER
                || this == CANCELLED_BY_STAFF
                || this == CANCELLED_PAYMENT_TIMEOUT;
    }

    public static List<BookingStatus> cancelledStatuses() {
        return List.of(
                CANCELLED,
                CANCELLED_BY_CUSTOMER,
                CANCELLED_BY_STAFF,
                CANCELLED_PAYMENT_TIMEOUT);
    }
}
