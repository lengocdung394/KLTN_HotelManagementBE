package iuh.fit.se.hotelmanagement_be.modular.booking.responses.bookingcancel;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingCancellationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingCancellationResponse {
    private String id;
    private String bookingId;
    private String customerId;
    private String customerName;
    private String customerPhone;
    private BookingCancellationStatus status;
    private String reason;
    private BigDecimal paidAmount;
    private BigDecimal refundAmount;
    private BigDecimal roomRefundAmount;
    private BigDecimal serviceRefundAmount;
    private BigDecimal roomPenaltyAmount;
    private long hoursBeforeCheckIn;
    private String refundPolicy;
    private String refundEmail;
    private String bankName;
    private String bankCode;
    private String bankAccountName;
    private String bankAccountNumber;
    private String reviewNote;
    private String refundReference;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime refundedAt;
}
