package iuh.fit.se.hotelmanagement_be.modular.promotion.requests;

import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request thay đổi trạng thái khuyến mãi")
public class ChangeStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    @Schema(example = "ACTIVE", allowableValues = {"DRAFT", "ACTIVE", "INACTIVE", "EXPIRED"})
    PromotionStatus status;

    @Size(max = 500)
    @Schema(example = "Kích hoạt cho chiến dịch hè 2025")
    String reason;
}
