package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingResponse {
    Long bookingId;
    Long customerId;
    String customerName;
    BookingStatus bookingStatus;
    BookingChannel bookingChannel;
    LocalDateTime createdAt;
    // --- Bổ sung thêm các trường tiền nong từ Order ---
    BigDecimal roomTotal;       // Tổng tiền phòng
    BigDecimal serviceTotal;    // Tổng tiền dịch vụ
    BigDecimal discountTotal;   // Số tiền được giảm giá
    BigDecimal finalAmount;     // Tổng tiền cuối cùng phải thanh toán
    List<BookingDetailResponse> bookingDetails;
}
