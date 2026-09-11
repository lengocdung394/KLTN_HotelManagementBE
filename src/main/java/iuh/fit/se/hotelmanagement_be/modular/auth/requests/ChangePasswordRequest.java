package iuh.fit.se.hotelmanagement_be.modular.auth.requests;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request doi mat khau")
public class ChangePasswordRequest {
    @NotBlank(message = "Mat khau hien tai khong duoc de trong")
    @Schema(example = "matkhaucu123")
    String currentPassword;

    @NotBlank(message = "Mat khau moi khong duoc de trong")
    @Size(min = 6, message = "Mat khau moi phai co it nhat 6 ky tu")
    @Schema(example = "matkhaumoi456")
    String newPassword;

    @NotBlank(message = "Xac nhan mat khau khong duoc de trong")
    @Schema(example = "matkhaumoi456")
    String confirmPassword;
}