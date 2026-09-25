package iuh.fit.se.hotelmanagement_be.modular.promotion.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionGetListByCustomerResponse {
    String id;
    String code;
    String name;
    String description;          // Mô tả chi tiết mã giảm giá
    String type;                 // Loại giảm giá (ROOM_PERCENTAGE, SERVICE_PERCENTAGE,...)
    BigDecimal discountValue;        // Giá trị giảm (% hoặc số tiền)
    BigDecimal maxDiscountAmount;    // Số tiền giảm tối đa
    BigDecimal minBookingValue;      // Giá trị đơn hàng tối thiểu để áp dụng
    LocalDateTime startDate;     // Ngày bắt đầu
    LocalDateTime endDate;       // Ngày hết hạn
    boolean exclusive;           // Có độc quyền / áp dụng riêng không
}
