package iuh.fit.se.hotelmanagement_be.modular.room.responses;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.Amenity;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomImage;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomStatus;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomCreateResponse {
    Long id;
    Long floorId;
    String floorName;
    RoomStatus roomStatus;
    RoomType roomType;
    Double basePrice;
    Double totalAmenitiesPrice;
    Double totalPrice;

    String defaultImageUrl;      // 👈 URL ảnh đại diện chính (isDefault = true)
    List<RoomImage> avatarUrl;   // 👈 Danh sách toàn bộ ảnh đã upload
    Set<Amenity> amenities;


}
