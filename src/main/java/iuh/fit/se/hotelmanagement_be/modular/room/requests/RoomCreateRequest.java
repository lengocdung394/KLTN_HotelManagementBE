package iuh.fit.se.hotelmanagement_be.modular.room.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomStatus;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomCreateRequest {
    @NotNull(message = "Tầng không được để trống!")
    @Schema(description = "ID tầng thuộc chi nhánh", example = "1")
    Long floorId;

    @NotNull(message = "Trạng thái phòng không được để trống!")
    @Schema(description = "Trạng thái hiện tại của phòng", example = "AVAILABLE")
    RoomStatus roomStatus;

    @NotNull(message = "Loại phòng không được để trống!")
    @Schema(description = "Loại phòng khách sạn", example = "DELUXE")
    RoomType roomType;

    @NotNull(message = "Giá gốc của phòng không được để trống!")
    @Min(value = 0, message = "Giá gốc của phòng phải lớn hơn hoặc bằng 0!")
    @Schema(description = "Giá gốc của phòng chưa tính tiện ích", example = "500000.00")
    Double basePrice;

    @Min(value = 0, message = "Vị trí ảnh đại diện phải lớn hơn hoặc bằng 0!")
    @Builder.Default
    @Schema(description = "Vị trí ảnh làm đại diện trong mảng upload (bắt đầu từ 0)", example = "0", defaultValue = "0")
    Integer defaultImageIndex = 0;

    @Schema(description = "Danh sách ID các tiện ích được chọn", example = "[1, 2, 5]")
    Set<Long> amenityIds;
}
