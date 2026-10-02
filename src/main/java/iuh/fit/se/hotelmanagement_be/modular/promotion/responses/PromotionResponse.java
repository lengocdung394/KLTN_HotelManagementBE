package iuh.fit.se.hotelmanagement_be.modular.promotion.responses;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionDiscountType;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionStatus;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionScope;
import jakarta.persistence.Column;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromotionResponse {

    String id;
    String code;
    String name;
    String description;
    PromotionScope type;
    PromotionDiscountType promotionDiscountType;
    BigDecimal discountValue;

    BigDecimal maxDiscountAmount; // tien toi da duoc giam

    BigDecimal minBookingValue; // tong tien booking toi thieu de duoc giam

    BigDecimal minRoomValue;    // Tổng tiền phòng tối thiểu (để kích hoạt mã riêng cho phòng)

    BigDecimal minServiceValue; // Tổng tiền dịch vụ tối thiểu (để kích hoạt mã riêng cho dịch vụ)

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime startDate;    // ngay bat dau

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime endDate;   // ngay ket yhuc

    Integer usageLimit;

    Integer usedCount;

    PromotionStatus status;

    boolean available;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt;

    String imageUrl;
}
