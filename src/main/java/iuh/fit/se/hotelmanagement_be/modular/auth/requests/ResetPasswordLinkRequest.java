package iuh.fit.se.hotelmanagement_be.modular.auth.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Đặt lại mật khẩu bằng liên kết xác thực qua email")
public class ResetPasswordLinkRequest {
    @NotBlank(message = "Token đặt lại mật khẩu không được để trống")
    String token;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    @Schema(example = "matkhaumoi123")
    String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    String confirmPassword;
}
