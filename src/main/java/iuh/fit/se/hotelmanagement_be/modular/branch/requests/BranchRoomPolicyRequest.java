package iuh.fit.se.hotelmanagement_be.modular.branch.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import jakarta.persistence.Column;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BranchRoomPolicyRequest {
    // Nếu dùng cho trường hợp tạo mới thì cần thông tin chi nhánh và loại phòng
    // (Nếu API chỉ dùng để cập nhật theo policyId thì có thể để optional hoặc bỏ qua)
    Long hotelId;

    RoomType roomType;

    @DecimalMin(value = "0.0", message = "Phí phụ thu người lớn không được âm")
    Double extraAdultFee;

    @DecimalMin(value = "0.0", message = "Phí phụ thu trẻ em không được âm")
    Double extraChildFee;

    @Min(value = 1, message = "Sức chứa tiêu chuẩn tối thiểu là 1")
    Integer standardCapacity;

    @Min(value = 0, message = "Sức chứa phụ thu tối đa không được âm")
    Integer maxExtraGuests;
    @JsonProperty("price")
    @DecimalMin(value = "0.0", message = "Giá cơ bản không được âm")
    Double basePrice;

    @DecimalMin(value = "0.0", message = "Diện tích phòng không được âm")
    Double area; // Đơn vị: m² (Ví dụ: 25.5, 30.0)
}
