package iuh.fit.se.hotelmanagement_be.modular.payment.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebhookResponse {
    int error;
    String message;
}
