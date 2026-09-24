package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateServiceQuantityRequest {
    // Mã phòng trong đơn đặt phòng (BookingDetail ID)
    Long bookingDetailId;

    // Danh sách dịch vụ cần cập nhật số lượng trong phòng đó
    List<ServiceQuantityItem> services;
}
