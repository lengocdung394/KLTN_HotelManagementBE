package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnpaidServiceItemResponse {

    Long bookingServiceId;
    String serviceName;
    int quantity;
    Double price;
    Double subTotal; // quantity * price
    LocalDateTime usedAt;
}
