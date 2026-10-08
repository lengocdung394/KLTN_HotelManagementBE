package iuh.fit.se.hotelmanagement_be.modular.auth.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminAccountRequest {

    @NotBlank(message = "Tên tài khoản admin không được để trống")
    String username;

    @NotBlank(message = "Mật khẩu admin không được để trống")
    String password;
}
