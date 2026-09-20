package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomServiceAdditionRequest {
    Long bookingDetailId; // Phòng phát sinh dịch vụ
    List<NewServiceRequest> services; // Danh sách dịch vụ thêm vào
}
