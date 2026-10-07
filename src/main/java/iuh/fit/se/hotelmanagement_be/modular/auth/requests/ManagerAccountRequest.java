package iuh.fit.se.hotelmanagement_be.modular.auth.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ManagerAccountRequest {
    @NotBlank(message = "Họ tên quản lý không được để trống")
    String fullName;

    @NotBlank(message = "Email quản lý không được để trống")
    @Email(message = "Email quản lý không hợp lệ")
    String email;

    @NotBlank(message = "Số điện thoại quản lý không được để trống")
    @Pattern(
            regexp = "^\\+?[0-9. ()-]{10,25}$",
            message = "Số điện thoại quản lý không hợp lệ"
    )
    String phone;

    @NotBlank(message = "Mật khẩu quản lý không được để trống")
    String password;
}
