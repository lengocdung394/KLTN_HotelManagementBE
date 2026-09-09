package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingDetailResponse {
    Long bookingDetailId;
    Long roomId;
    String roomName;
    String roomTypeName;
    LocalDateTime checkInTime;
    LocalDateTime checkOutTime;
    int numAdults;
    int numChildren;
    int numInfants;
    double price;

}
