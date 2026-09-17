package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutSummaryResponse {
    Long bookingId;
    Long orderId;
    String customerName;
    // Các khoản tiền tổng quan
    BigDecimal roomTotal;
    BigDecimal initialServiceTotal;
    BigDecimal discountTotal;
    BigDecimal totalOrderAmount;     // Tổng tiền hóa đơn sau khi cộng dịch vụ phát sinh
    BigDecimal paidAmount;           // Tiền khách đã trả lúc đặt online/quầy (100%)
    BigDecimal remainingAmountToPay; // Số tiền cần thanh toán lúc checkout (tiền dịch vụ phát sinh)
    // Danh sách các dịch vụ phát sinh chưa trả tiền
    List<UnpaidServiceItemResponse> unpaidServices;

}