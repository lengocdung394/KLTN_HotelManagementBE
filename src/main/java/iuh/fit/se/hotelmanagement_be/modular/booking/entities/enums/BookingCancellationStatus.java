package iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums;

public enum BookingCancellationStatus {
    REFUND_BANK_DETAILS_REQUIRED, // Cần bổ sung thông tin ngân hàng
    REFUND_REVIEW_PENDING, // REFUND_REVIEW_PENDING
    REFUND_APPROVED, // REFUND_APPROVED
    REFUND_REJECTED, // REFUND_REJECTED
    REFUNDED, // Đã hoàn tiền thành công
    NO_REFUND_REQUIRED // NO_REFUND_REQUIRED
}
