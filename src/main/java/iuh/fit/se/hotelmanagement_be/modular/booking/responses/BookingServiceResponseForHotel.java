package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingServiceResponseForHotel {

    String serviceId;
    String name;
    Integer quantity;
    Double price;
    LocalDateTime usedAt;
    Boolean cancelled;
    LocalDateTime cancelledAt;
}
