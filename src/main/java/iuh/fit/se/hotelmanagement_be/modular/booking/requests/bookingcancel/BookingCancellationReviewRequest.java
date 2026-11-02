package iuh.fit.se.hotelmanagement_be.modular.booking.requests.bookingcancel;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingRefundTransferMethod;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingCancellationReviewRequest {
    @Size(max = 500)
    String note;
    Boolean refundAmountVerified;
    Boolean refundAccountConfirmed;
    BookingRefundTransferMethod refundTransferMethod;
    @Size(max = 100)
    String transactionReference;
}
