package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutRefundBankDetailsRequest {
    @Size(max = 120)
    String bankName;

    @Size(max = 30)
    String bankCode;

    @Size(max = 120)
    String bankAccountName;

    @Size(max = 50)
    String bankAccountNumber;
}
