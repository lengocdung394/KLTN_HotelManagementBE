package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceQuantityItem {
    String serviceId; // Hoặc bookingServiceDetailId tùy vào việc bạn quản lý theo loại dịch vụ hay dòng dịch vụ chi tiết
    int quantity;   // Số lượng thực tế mới (nếu = 0 thì hiểu là hủy dịch vụ này)

}
