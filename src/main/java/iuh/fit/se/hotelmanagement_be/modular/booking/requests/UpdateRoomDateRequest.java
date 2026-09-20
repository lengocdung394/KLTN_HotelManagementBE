package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateRoomDateRequest {

    Long bookingDetailId;
    LocalDateTime newCheckinTime;
    LocalDateTime newCheckoutTime;
}
