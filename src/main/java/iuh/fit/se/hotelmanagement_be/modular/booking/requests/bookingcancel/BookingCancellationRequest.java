package iuh.fit.se.hotelmanagement_be.modular.booking.requests.bookingcancel;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingCancellationRefundMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingCancellationRequest {
    @Size(max = 500)
    String reason; // nguyen nhan huy booking

    BookingCancellationRefundMethod refundMethod;
    Boolean cashRefundConfirmed;
    @DecimalMin("0.00")
    BigDecimal confirmedRefundAmount;
    @DecimalMin("0.00")
    BigDecimal confirmedRoomRefundAmount;
    @DecimalMin("0.00")
    BigDecimal confirmedServiceRefundAmount;
    @DecimalMin("0.00")
    BigDecimal confirmedRoomPenaltyAmount;
    String confirmedRefundPolicy;

    @Size(max = 120)
    String bankName;

    @Size(max = 30)
    String bankCode;

    @Size(max = 120)
    String bankAccountName;

    @Size(max = 50)
    String bankAccountNumber;

    @Email
    @Size(max = 254)
    private String refundEmail;
}
