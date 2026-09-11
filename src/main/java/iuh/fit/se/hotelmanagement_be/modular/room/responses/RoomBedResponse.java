package iuh.fit.se.hotelmanagement_be.modular.room.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomBedResponse {
    String bedTypeName;      // Tên loại giường (VD: Queen Bed, Single Bed)
    String description;      // Mô tả giường
    Integer quantity;        // Số lượng giường loại này trong phòng
    Integer capacity;        // Sức chứa của 1 giường
    Boolean isExtraBed;      // Có phải giường phụ không
}
