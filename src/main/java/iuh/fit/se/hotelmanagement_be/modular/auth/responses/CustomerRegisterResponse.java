package iuh.fit.se.hotelmanagement_be.modular.auth.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerRegisterResponse {
    String cccd;
    String email;
    String status;
    String  message;

}
