package iuh.fit.se.hotelmanagement_be.modular.branch.controllers;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomTypeDetailResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.services.RoomService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HotelRoomPolicyController {

    private final RoomService roomService; // Hoặc Service tương ứng của bạn

    @GetMapping("/{hotelId}/room-types/{roomType}/detail")
    public ResponseEntity<RoomTypeDetailResponse> getRoomTypeDetail(
            @PathVariable Long hotelId,
            @PathVariable RoomType roomType) {

        RoomTypeDetailResponse response = roomService.getRoomTypeDetailByHotelAndType(hotelId, roomType);
        return ResponseEntity.ok(response);
    }
}
