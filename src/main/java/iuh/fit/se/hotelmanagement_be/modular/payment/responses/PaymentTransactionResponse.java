package iuh.fit.se.hotelmanagement_be.modular.payment.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentTransactionResponse {
    Long transactionId;      // ID của giao dịch thanh toán vừa tạo
    String orderId;            // ID của hóa đơn
    BigDecimal totalAmount;  // Tổng tiền hóa đơn cần trả
    BigDecimal amountPaid;   // Số tiền thực tế khách đưa
    BigDecimal changeAmount; // Số tiền thừa (tiền thối lại cho khách)
    String paymentType;      // Hình thức thanh toán (CASH, BANK,...)
    String status;           // Trạng thái mới của hóa đơn (PAID, CLOSED,...)
    LocalDateTime transactionDate; // Thời điểm giao dịch
    String message;          // Thông báo thành công

}
