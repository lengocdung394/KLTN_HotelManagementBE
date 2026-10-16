package iuh.fit.se.hotelmanagement_be.modular.payment.responses;

import iuh.fit.se.hotelmanagement_be.modular.payment.entities.CheckoutRefundStatus;
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
public class CheckoutRefundResponse {
    String id;
    String bookingId;
    String customerName;
    String customerPhone;
    Long hotelId;
    BigDecimal paidAmount;
    BigDecimal finalAmount;
    BigDecimal refundAmount;
    PaymentType refundMethod;
    CheckoutRefundStatus status;
    String bankName;
    String bankCode;
    String bankAccountName;
    String bankAccountNumber;
    String note;
    LocalDateTime requestedAt;
    LocalDateTime approvedAt;
    LocalDateTime refundedAt;
}
