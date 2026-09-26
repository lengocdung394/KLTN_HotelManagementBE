package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomChangeRequest {
    String bookingDetailId; // Phòng hiện tại trong booking cần đổi
    Long newRoomId;       // ID của phòng mới muốn đổi sang
}
