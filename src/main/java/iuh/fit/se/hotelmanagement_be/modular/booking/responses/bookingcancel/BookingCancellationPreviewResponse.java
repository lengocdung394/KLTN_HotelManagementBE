package iuh.fit.se.hotelmanagement_be.modular.booking.responses.bookingcancel;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingCancellationPreviewResponse {
    String bookingId;
    BigDecimal paidAmount;
    BigDecimal refundAmount;
    BigDecimal roomRefundAmount;
    BigDecimal serviceRefundAmount;
    BigDecimal roomPenaltyAmount;
    long hoursBeforeCheckIn;
    String refundPolicy;
}
