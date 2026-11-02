package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConfirmServiceUsageRequest {
    @NotNull(message = "Số lượng dịch vụ xác nhận không được để trống")
    @Min(value = 1, message = "Số lượng dịch vụ xác nhận phải lớn hơn 0")
    Integer quantity;
}
