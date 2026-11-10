package iuh.fit.se.hotelmanagement_be.modular.auth.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SuperAdminProfileUpdateRequest {
    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 150, message = "Họ tên không được vượt quá 150 ký tự")
    String fullName;
}
