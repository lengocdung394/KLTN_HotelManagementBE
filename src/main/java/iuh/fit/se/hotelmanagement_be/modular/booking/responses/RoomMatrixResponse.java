package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomMatrixResponse {

    String roomId;
    String roomNumber;        // Số phòng (Ví dụ: P.101)
    String roomTypeName;      // Tên loại phòng (Ví dụ: Deluxe, VIP)

    // Danh sách lịch bận của riêng phòng này trong khoảng thời gian tra cứu
    List<RoomScheduleDto> schedules;
}
