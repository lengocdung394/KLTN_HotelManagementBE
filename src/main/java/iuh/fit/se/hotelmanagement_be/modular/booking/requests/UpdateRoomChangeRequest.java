package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateRoomChangeRequest {
    Long bookingDetailId; // Phòng hiện tại trong booking đang ở
    Long newRoomId;       // ID của phòng mới muốn đổi sang
}
