package iuh.fit.se.hotelmanagement_be.modular.auth.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmployeeProfileUpdateRequest {
    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    String currentPassword;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    String email;

    @NotBlank(message = "Họ và tên không được để trống")
    String fullName;

    String phone;
    String cccd;
    String address;
    String position;
    String newPassword;
    String confirmPassword;
}
