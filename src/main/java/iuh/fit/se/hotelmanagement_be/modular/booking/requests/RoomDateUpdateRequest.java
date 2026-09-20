package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomDateUpdateRequest {
    Long bookingDetailId;       // Phòng cần thay đổi thời gian
    LocalDateTime newCheckInTime;  // Thời gian check-in mới
    LocalDateTime newCheckoutTime; // Thời gian check-out mới
}
