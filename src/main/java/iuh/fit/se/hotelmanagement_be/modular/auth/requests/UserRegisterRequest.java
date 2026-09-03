package iuh.fit.se.hotelmanagement_be.modular.auth.requests;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRegisterRequest {
    String email;
    String phone;
    String fullName;
    String address;
    String cccd;
    String position;
    Long hotelId;
    String password;
}
