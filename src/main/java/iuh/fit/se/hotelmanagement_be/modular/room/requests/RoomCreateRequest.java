package iuh.fit.se.hotelmanagement_be.modular.room.requests;

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

    @NotNull(message = "Trạng thái phòng không được để trống!")
    RoomStatus roomStatus;

    @NotNull(message = "Loại phòng không được để trống!")
    RoomType roomType;

    @NotNull(message = "Giá gốc của phòng không được để trống!")
    @Min(value = 0, message = "Giá gốc của phòng phải lớn hơn hoặc bằng 0!")
    Double basePrice;

    // Danh sách ID các Tiện ích được chọn (Ví dụ: [1, 2, 5])
    Set<Long> amenityIds;
    Long floorId;
    @Builder.Default
    Integer defaultImageIndex = 0;
}
