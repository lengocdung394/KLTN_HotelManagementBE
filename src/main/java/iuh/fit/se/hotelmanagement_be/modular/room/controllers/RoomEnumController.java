package iuh.fit.se.hotelmanagement_be.modular.room.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomStatus;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomType;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/rooms/enums")
@Tag(name = "Room Enum Management", description = "APIs lấy thông tin các Enum liên quan đến Phòng")
public class RoomEnumController {
    @Operation(summary = "Lấy danh sách tất cả loại phòng (RoomType)")
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<RoomType>>> getAllRoomTypes() {
        List<RoomType> roomTypes = Arrays.asList(RoomType.values());
        return ResponseEntity.ok(ApiResponse.<List<RoomType>>builder()
                .code(200)
                .message("Lay danh ds loai phong thanh cong")
                .result(roomTypes)
                .build());

    }

    @Operation(summary = "Lay danh sach trang thai phong (RoomStatus)")
    @GetMapping("/statuses")
    public ResponseEntity<ApiResponse<List<RoomStatus>>> getAllRoomStatuses() {

        List<RoomStatus> roomStatuses = Arrays.asList(RoomStatus.values());

        return ResponseEntity.ok(ApiResponse.<List<RoomStatus>>builder()
                .code(200)
                .message("Lay ds trang thai phong thanh cong")
                .result(roomStatuses)
                .build()
        );
    }

}
