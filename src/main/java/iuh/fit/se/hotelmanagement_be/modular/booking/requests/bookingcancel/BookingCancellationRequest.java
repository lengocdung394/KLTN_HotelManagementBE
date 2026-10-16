package iuh.fit.se.hotelmanagement_be.modular.booking.requests.bookingcancel;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingCancellationRequest {
    @Size(max = 500)
    String reason; // nguyen nhan huy booking

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
