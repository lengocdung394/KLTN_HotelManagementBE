package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionRequest {
    String promotionId;
}
