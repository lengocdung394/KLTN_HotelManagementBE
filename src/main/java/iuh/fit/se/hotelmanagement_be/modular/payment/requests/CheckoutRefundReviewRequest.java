package iuh.fit.se.hotelmanagement_be.modular.payment.requests;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutRefundReviewRequest {
    @Size(max = 500)
    String note;
    Boolean refundAmountVerified;
    Boolean refundAccountConfirmed;
    @Size(max = 120)
    String bankName;
    @Size(max = 30)
    String bankCode;
    @Size(max = 120)
    String bankAccountName;
    @Size(max = 50)
    String bankAccountNumber;
}
