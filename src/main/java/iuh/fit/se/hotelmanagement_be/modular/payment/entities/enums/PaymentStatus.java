package iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums;

public enum PaymentStatus {
    PAID, // Đã thanh toán đủ số tiền của hóa đơn
    PARTIAL, // Đã thanh toán một phần, vẫn còn số tiền cần thanh toán
    UNPAID // Chưa thanh toán
}
