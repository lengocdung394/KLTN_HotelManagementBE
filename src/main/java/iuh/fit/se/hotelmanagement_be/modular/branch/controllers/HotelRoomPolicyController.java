package iuh.fit.se.hotelmanagement_be.modular.branch.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerGetOneResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.AuthService;
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

import java.util.List;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Hotel", description = "APIs liên quan đến quản lý khách sạn")
public class HotelRoomPolicyController {

    private final RoomService roomService; // Hoặc Service tương ứng của bạn
    private final AuthService authService;

    @GetMapping("/{hotelId}/room-types/{roomType}/detail")
    public ResponseEntity<RoomTypeDetailResponse> getRoomTypeDetail(
            @PathVariable Long hotelId,
            @PathVariable RoomType roomType) {

        RoomTypeDetailResponse response = roomService.getRoomTypeDetailByHotelAndType(hotelId, roomType);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Lấy danh sách tất cả khách hàng có trong hệ thống")
    @GetMapping("/getAllCustomer")
    public ResponseEntity<List<CustomerGetOneResponse>> getAllCustomers() {
        return ResponseEntity.ok(authService.getAllCustomers());
    }

    @Operation(summary = "Lấy danh sách khách hàng theo chi nhánh khách sạn dựa trên lịch sử đặt phòng")
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<CustomerGetOneResponse>> getCustomersByHotelId(@PathVariable Long hotelId) {
        return ResponseEntity.ok(authService.getCustomersByHotelId(hotelId));
    }
}
