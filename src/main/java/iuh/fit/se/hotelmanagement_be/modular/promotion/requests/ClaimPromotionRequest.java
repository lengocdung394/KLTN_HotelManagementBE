package iuh.fit.se.hotelmanagement_be.modular.promotion.requests;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClaimPromotionRequest {
    @NotNull(message = "ID khuyến mãi không được để trống")
    Long promotionId;
}
