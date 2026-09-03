package iuh.fit.se.hotelmanagement_be.modular.auth.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreateResponse {
    Long id;
    String email;
    String fullName;
    String phone;
    String cccd;
    String address;
    String position;
    String hotelName;
    Set<String> roles;


}
