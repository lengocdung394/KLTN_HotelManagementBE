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
public class BookingResponseForHotel {
    String orderId;
    String bookingId;
    String customerId;
    String customerName;
    Long hotelId;
    BookingStatus bookingStatus;
    BookingChannel bookingChannel;
    LocalDateTime createdAt;
    // Tiền đã thanh toán
    BigDecimal  paidAmount;
    BigDecimal  remainingAmount;

    // Lam ro phan tien giam giá
    BigDecimal discountRoomAmount;     // Số tiền được giảm cho phòng
    BigDecimal discountServiceAmount;  // Số tiền được giảm cho dịch vụ
    BigDecimal discountAmountTotal;    // Số tiền giảm trừ thẳng vào tổng hóa đơn chung

    // Tiền thiếu lại khách hàng nếu có
    // --- Bổ sung thêm các trường tiền nong từ Order ---
    BigDecimal roomTotal;       // Tổng tiền phòng
    BigDecimal serviceTotal;    // Tổng tiền dịch vụ
    BigDecimal discountTotal;   // Số tiền được giảm giá
    BigDecimal finalAmount;     // Tổng tiền cuối cùng phải thanh toán
    List<BookingDetailResponseForHotel> bookingDetails;
}
