package iuh.fit.se.hotelmanagement_be.modular.room.responses;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.Amenity;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomImage;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomResponse {
    Long id;
    Long floorId;
    int floorNumber;
    String nameBuilding;
    RoomStatus roomStatus;
    RoomType roomType;
    Double basePrice;
    Double totalAmenitiesPrice;
    Double totalPrice;
    List<RoomBedResponse> beds;
    String defaultImageUrl;      // URL ảnh đại diện chính (isDefault = true)
    List<RoomImage> avatarUrl;   // Danh sách toàn bộ ảnh đã upload
    Set<Amenity> amenities;

    //Thêm các thông tin chính sách phòng (Policy)
    Integer standardAdults;
    Integer maxAdults;
    Integer maxChildren;
    Integer maxInfants;
    Double extraAdultFee;
    Double extraChildFee;
}
