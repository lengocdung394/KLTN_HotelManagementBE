package iuh.fit.se.hotelmanagement_be.modular.auth.requests;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRegisterRequest {
    String password;
    String phone;
    String email;
    String fullName;
}
