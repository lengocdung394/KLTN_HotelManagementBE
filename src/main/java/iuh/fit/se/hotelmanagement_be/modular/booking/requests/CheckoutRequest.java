package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutRequest {
    @NotEmpty
    List<String> bookingDetailIds;

    PaymentType surplusRefundMethod;

    @Size(max = 500)
    String refundNote;
}
