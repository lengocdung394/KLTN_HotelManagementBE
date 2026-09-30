package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomUpdateRequest {
    private String bookingDetailId;
    private String newRoomId;             // Null nếu không đổi phòng
    private LocalDateTime newCheckInTime;  // Null nếu không đổi ngày check-in
    private LocalDateTime newCheckoutTime; // Null nếu không đổi ngày check-out
    private Integer numAdults;          // Null nếu không đổi số lượng người lớn
    private Integer numChildren;        // Null nếu không đổi trẻ em
    private Integer numInfants;         // Null nếu không đổi trẻ sơ sinh
}
