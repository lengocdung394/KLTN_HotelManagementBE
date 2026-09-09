package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingCreateRequest {
    // Thông tin khách hàng
    @NotNull(message = "Mã khách hàng không được để trống")
    Long customerId;

    // Thông tin nhân viên (Nếu như là đặt tài quầy)
    @NotNull(message = "Mã nhân viên không được để trống")
    Long employeeId;

    @NotNull(message = "Tầng/Phòng không được để trống")
    Long roomId;

    @NotNull(message = "Thời gian nhận phòng không được để trống")
    LocalDateTime checkinTime;

    @NotNull(message = "Thời gian trả phòng không được để trống")
    LocalDateTime checkoutTime;

    BookingChannel bookingChannel; // WALK_IN hoặc ONLINE

    // Giá phòng thời điểm đó
    Long customerPromotionId; // Voucher của khách (nếu có)

    Long promotionId;         // Mã giảm giá chung (nếu có)

    @NotEmpty(message = "Danh sách chi tiết phòng đặt không được để trống")
    @Valid
    List<BookingDetailCreateRequest> bookingDetails; // Đúng thiết kế 1 - N
}
