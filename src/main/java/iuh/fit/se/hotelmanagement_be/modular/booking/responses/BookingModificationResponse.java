package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingModificationResponse {

    String bookingId;
    BigDecimal oldTotalAmount;
    BigDecimal newTotalAmount;
    BigDecimal totalChange;
}
