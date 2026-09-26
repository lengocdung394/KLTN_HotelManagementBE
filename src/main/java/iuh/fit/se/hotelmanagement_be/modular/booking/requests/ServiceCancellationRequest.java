package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceCancellationRequest {
    String bookingDetailId;         // Phòng nào?
    List<Long> serviceDetailIds;  // Các ID dịch vụ trong phòng đó cần hủy
}
