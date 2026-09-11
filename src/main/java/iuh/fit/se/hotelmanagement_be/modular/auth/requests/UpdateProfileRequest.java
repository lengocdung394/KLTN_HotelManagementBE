package iuh.fit.se.hotelmanagement_be.modular.auth.requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request cập nhật hồ sơ cá nhân")
public class UpdateProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự")
    @Schema(example = "Nguyễn Văn Huy")
    String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không hợp lệ")
    @Schema(example = "0901234567")
    String phone;

    @Size(max = 12, message = "Số CCCD/CMND không hợp lệ")
    @Schema(example = "001234567890")
    String cccd;

    @JsonFormat(pattern = "dd/MM/yyyy")
    @Schema(example = "01/01/2000")
    LocalDate dateOfBirth;
}
