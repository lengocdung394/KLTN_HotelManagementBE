package iuh.fit.se.hotelmanagement_be.modular.auth.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Yêu cầu cập nhật hồ sơ khách hàng")
public class CustomerUpdateProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Schema(example = "Nguyễn Văn A")
    String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Schema(example = "0912345678")
    String phone;

    @Schema(example = "001234567890")
    String cccd;

    @Schema(example = "2000-01-01")
    String birthDate;

    @Schema(example = "2000-01-01")
    String dateOfBirth;
}
