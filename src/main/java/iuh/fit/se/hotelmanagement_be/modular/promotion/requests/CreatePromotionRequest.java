package iuh.fit.se.hotelmanagement_be.modular.promotion.requests;

import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionStatus;
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
@Schema(description = "Request tạo mới khuyến mãi")
public class CreatePromotionRequest {

    @NotBlank(message = "Mã khuyến mãi không được để trống")
    @Size(min = 3, max = 50, message = "Mã khuyến mãi phải từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Mã chỉ chứa chữ IN HOA, số, gạch ngang và gạch dưới")
    @Schema(example = "SUMMER2025")
    String code;

    @NotBlank(message = "Tên khuyến mãi không được để trống")
    @Size(max = 200, message = "Tên không được vượt quá 200 ký tự")
    @Schema(example = "Ưu đãi hè 2025")
    String name;

    @Size(max = 2000)
    String description;

    @NotNull(message = "Loại khuyến mãi không được để trống")
    @Schema(example = "PERCENTAGE")
    PromotionType type;

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
    @Schema(example = "20.00")
    BigDecimal discountValue;

    @DecimalMin(value = "0", message = "Giảm tối đa không được âm")
    @Schema(example = "500000")
    BigDecimal maxDiscountAmount;

    @DecimalMin(value = "0", message = "Giá trị booking tối thiểu không được âm")
    @Schema(example = "1000000")
    BigDecimal minBookingValue;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @Schema(example = "2025-07-01T00:00:00")
    LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @Schema(example = "2025-08-31T23:59:59")
    LocalDateTime endDate;

    @Min(value = 1, message = "Số lần dùng tối đa phải >= 1")
    @Schema(example = "200")
    Integer usageLimit;

    @Schema(example = "DRAFT", defaultValue = "DRAFT")
    PromotionStatus status = PromotionStatus.DRAFT;
}
