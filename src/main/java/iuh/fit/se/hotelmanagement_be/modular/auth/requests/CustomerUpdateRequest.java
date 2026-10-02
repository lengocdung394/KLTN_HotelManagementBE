package iuh.fit.se.hotelmanagement_be.modular.auth.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerUpdateRequest {

    @Schema(description = "Họ và tên khách hàng", example = "Nguyễn Văn A")
    String name;

    @Schema(description = "Số điện thoại", example = "0901234567")
    String phone;

    @Schema(description = "Email", example = "nguyenvana@gmail.com")
    String email;

    @Schema(description = "Số CCCD/CMND/Passport", example = "079202001234")
    String identityNumber;

    @Schema(description = "Ghi chú về khách hàng", example = "Khách quen, thích phòng tầng cao")
    String note;
}
