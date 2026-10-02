package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LateCheckOutBookingNotificationResponse {

    String bookingId;
    String customerName; // Tên khách hàng đại diện đặt phòng
    List<LateRoomDetailResponse> lateRoomDetails; // Danh sách các phòng trễ hạn trong booking này
}
