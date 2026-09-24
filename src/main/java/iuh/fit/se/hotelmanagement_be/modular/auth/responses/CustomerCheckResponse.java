package iuh.fit.se.hotelmanagement_be.modular.auth.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerCheckResponse {

    String fullName;
    String cccd;
    String phone;
    String status;
    String message;
    String email;
}
