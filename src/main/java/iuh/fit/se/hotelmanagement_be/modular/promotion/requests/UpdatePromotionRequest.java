package iuh.fit.se.hotelmanagement_be.modular.promotion.requests;

import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request cập nhật khuyến mãi")
public class UpdatePromotionRequest {

    @NotBlank(message = "Tên khuyến mãi không được để trống")
    @Size(max = 200)
    @Schema(example = "Ưu đãi hè 2025 - Cập nhật")
    String name;

    @Size(max = 2000)
    String description;

    @NotNull(message = "Loại khuyến mãi không được để trống")
    PromotionType type;

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.01")
    @Schema(example = "25.00")
    BigDecimal discountValue;

    @DecimalMin(value = "0")
    BigDecimal maxDiscountAmount;

    @DecimalMin(value = "0")
    BigDecimal minBookingValue;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    LocalDateTime endDate;

    @Min(value = 1)
    Integer usageLimit;
}
