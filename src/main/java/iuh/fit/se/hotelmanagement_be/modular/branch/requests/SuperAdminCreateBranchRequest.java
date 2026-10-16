package iuh.fit.se.hotelmanagement_be.modular.branch.requests;

import com.fasterxml.jackson.annotation.JsonAlias;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.AdminAccountRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ManagerAccountRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SuperAdminCreateBranchRequest {
    @NotBlank(message = "Tên chi nhánh không được để trống")
    String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    String address;

    @NotBlank(message = "Số điện thoại chi nhánh không được để trống")
    @Pattern(
            regexp = "^\\+?[0-9. ()-]{10,25}$",
            message = "Số điện thoại chi nhánh không hợp lệ"
    )
    String phone;

    @NotBlank(message = "Tên tỉnh/thành phố không được để trống")
    String provinceName;

    @Valid
    AdminAccountRequest adminAccount;

    @Valid
    ManagerAccountRequest managerAccount;
    @JsonAlias("roomPolicy")
    List<BranchRoomPolicyRequest> roomPolicies;
}
