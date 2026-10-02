package iuh.fit.se.hotelmanagement_be.modular.auth.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmployeeResponse {
    String id;
    String email;
    String fullName;
    String phone;
    String cccd;
    String address;
    String position;
    String avatarUrl;
    LocalDate dateOfBirth;
    Long hotelId;
    String hotelName;
    Set<String> roles;
}
