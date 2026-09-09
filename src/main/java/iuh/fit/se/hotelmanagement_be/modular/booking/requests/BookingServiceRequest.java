package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingServiceRequest {
    @NotNull(message = "ID dịch vụ không được để trống")
    @Schema(description = "ID Dịch vụ được chọn", example = "1")
    Long serviceId;

    @NotNull(message = "Số lượng dịch vụ không được để trống")
    @Positive(message = "Số lượng dịch vụ phải lớn hơn 0")
    @Schema(description = "Số lượng sử dụng", example = "2")
    Integer quantity;

    @Min(value = 0, message = "Đơn giá dịch vụ phải lớn hơn hoặc bằng 0")
    @Schema(description = "Đơn giá dịch vụ tại thời điểm đăng ký (có thể để null để Backend tự lấy giá hiện tại trong DB)", example = "50000.00")
    Double price;

    @Schema(description = "Thời gian sử dụng/đăng ký dịch vụ (có thể để null để Backend tự lấy thời gian hiện tại)", example = "2026-09-10T15:30:00")
    LocalDateTime usedAt;
}
