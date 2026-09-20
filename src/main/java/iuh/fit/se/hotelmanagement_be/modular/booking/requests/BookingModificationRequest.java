package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingModificationRequest {
    Long employeeId;
    // 1. Phần HỦY PHÒNG & HỦY DỊCH VỤ LẺ
    List<Long> bookingDetailIdsToCancel; // Các phòng muốn hủy
    List<ServiceCancellationRequest> servicesToCancel;  // Hủy dịch vụ lẻ (đã gom theo từng phòng)

    // 2. Phần THÊM MỚI PHÒNG (Đã bao gồm dịch vụ đi kèm cho phòng đó nếu có)
    List<NewRoomRequest> roomsToAdd;

    // 3. Phần ĐỔI PHÒNG
    List<UpdateRoomChangeRequest> roomsToChange;

    List<RoomDateUpdateRequest> roomsToUpdateDates;

    // 5.PHẦN THÊM DỊCH VỤ PHÁT SINH cho phòng đang ở sẵn (Phòng cũ)
    List<RoomServiceAdditionRequest> servicesToAddForExistingRooms;
}
