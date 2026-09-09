package iuh.fit.se.hotelmanagement_be.modular.promotion.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Thông tin Voucher trong Ví của Khách hàng")
public class CustomerPromotionResponse {

    @Schema(description = "ID của bản ghi CustomerPromotion", example = "10")
    Long id;

    @Schema(description = "Mã voucher thực tế để khách nhập/áp dụng khi Booking", example = "AUTUMN2026")
    String voucherCode;

    @Schema(description = "Đã sử dụng chưa", example = "false")
    boolean isUsed;

    @Schema(description = "Thời gian khách nhận/lưu voucher", example = "2026-09-09T14:30:00")
    LocalDateTime savedAt;

    @Schema(description = "Thời gian sử dụng (null nếu chưa dùng)", example = "null")
    LocalDateTime usedAt;

    // --- Thông tin chi tiết lấy từ Promotion gốc ---

    @Schema(description = "ID khuyến mãi gốc", example = "1")
    Long promotionId;

    @Schema(description = "Tên chương trình khuyến mãi", example = "Ưu đãi Mùa Thu 2026")
    String name;

    @Schema(description = "Mô tả điều kiện sử dụng")
    String description;

    @Schema(description = "Loại giảm giá (PERCENTAGE / FIXED_AMOUNT)", example = "PERCENTAGE")
    PromotionType type;

    @Schema(description = "Giá trị giảm (% hoặc số tiền)", example = "15.00")
    BigDecimal discountValue;

    @Schema(description = "Số tiền giảm tối đa (với loại PERCENTAGE)", example = "300000")
    BigDecimal maxDiscountAmount;

    @Schema(description = "Giá trị đơn đặt phòng tối thiểu để được áp dụng", example = "800000")
    BigDecimal minBookingValue;

    @Schema(description = "Ngày bắt đầu có hiệu lực", example = "2026-10-01T00:00:00")
    LocalDateTime startDate;

    @Schema(description = "Ngày hết hạn voucher", example = "2026-10-31T23:59:59")
    LocalDateTime endDate;

    @Schema(description = "ID Khách sạn áp dụng (null nếu áp dụng Toàn hệ thống)", example = "1")
    Long hotelId;

    @Schema(description = "Tên Khách sạn áp dụng", example = "Chi nhánh Đà Nẵng")
    String hotelName;
}
