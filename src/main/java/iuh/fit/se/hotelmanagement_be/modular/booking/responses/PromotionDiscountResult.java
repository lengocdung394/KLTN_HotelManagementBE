package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionDiscountResult {
    BigDecimal discountRoomAmount;     // Số tiền được giảm cho phòng
    BigDecimal discountServiceAmount;  // Số tiền được giảm cho dịch vụ
    BigDecimal discountAmountTotal;    // Số tiền giảm trừ thẳng vào tổng hóa đơn chung
}
