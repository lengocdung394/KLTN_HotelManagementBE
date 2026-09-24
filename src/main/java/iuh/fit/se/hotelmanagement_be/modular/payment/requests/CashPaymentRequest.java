package iuh.fit.se.hotelmanagement_be.modular.payment.requests;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CashPaymentRequest {
    String orderId;
    BigDecimal amountPaid; // Số tiền khách đưa
    String note;           // Ghi chú (nếu có)
}
