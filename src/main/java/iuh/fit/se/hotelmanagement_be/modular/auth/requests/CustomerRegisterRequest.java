package iuh.fit.se.hotelmanagement_be.modular.auth.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerRegisterRequest {
    String email;
    String password;
    String phone;
    String fullName;
    @NotBlank(message = "CCCD không được để trống")
    @Pattern(regexp = "\\d{12}")
    String cccd;
}
