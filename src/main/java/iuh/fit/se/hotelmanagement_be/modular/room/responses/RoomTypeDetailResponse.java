package iuh.fit.se.hotelmanagement_be.modular.room.responses;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeDetailResponse {
    RoomType roomType; // Loại phòng (STANDARD, DELUXE,...)

    // --- 1. CHÍNH SÁCH QUY ĐỊNH (BranchRoomPolicy) ---
    Integer standardAdults;
    Integer maxAdults;
    Integer maxChildren;
    Integer maxInfants;
    Double extraAdultFee;
    Double extraChildFee;

    // --- 2. DANH SÁCH GIƯỜNG ĐI KÈM (RoomTypeBed) ---
    List<RoomBedResponse> beds;

}
