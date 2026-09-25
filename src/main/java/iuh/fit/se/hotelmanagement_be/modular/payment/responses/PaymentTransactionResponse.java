package iuh.fit.se.hotelmanagement_be.modular.payment.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Thông tin chi tiết giao dịch thanh toán")
public class PaymentTransactionResponse {
    Long id;
    Long orderId;
    BigDecimal amount;
    PaymentType paymentType;
    CashFlowType cashFlowType;
    String note;
    LocalDateTime createdAt;
}
