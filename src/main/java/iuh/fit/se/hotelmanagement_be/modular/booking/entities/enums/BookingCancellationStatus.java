package iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums;

public enum BookingCancellationStatus {
    REFUND_BANK_DETAILS_REQUIRED, // Cần bổ sung thông tin ngân hàng để hoàn tiền
    REFUND_REVIEW_PENDING, // Yêu cầu hoàn tiền đang chờ nhân viên duyệt
    REFUND_APPROVED, // Yêu cầu hoàn tiền đã được duyệt, chờ gửi lệnh payout
    REFUND_REJECTED, // Yêu cầu hoàn tiền bị nhân viên từ chối
    PAYOUT_PROCESSING, // Lệnh đã gửi PayOS; payoutState lưu trạng thái chi tiết hiện tại của PayOS
    PAYOUT_FAILED, // PayOS từ chối hoặc báo giao dịch payout thất bại
    REFUNDED, // PayOS hoàn tất và hệ thống đã xác minh giao dịch cùng số tiền
    NO_REFUND_REQUIRED // Booking bị hủy nhưng không cần hoàn tiền
}
