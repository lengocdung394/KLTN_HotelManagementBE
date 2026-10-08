package iuh.fit.se.hotelmanagement_be.modular.auth.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SuperAdminAccountUpdateRequest {

    @Email(message = "Email không đúng định dạng")
    @Size(max = 254, message = "Email không được vượt quá 254 ký tự")
    String email;

    @Size(max = 150, message = "Họ tên không được vượt quá 150 ký tự")
    String fullName;

    @Size(max = 30, message = "Số điện thoại không được vượt quá 30 ký tự")
    String phone;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    String address;

    @Size(max = 100, message = "Chức vụ không được vượt quá 100 ký tự")
    String position;

    String role;
}
