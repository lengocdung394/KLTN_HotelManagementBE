package iuh.fit.se.hotelmanagement_be.modular.branch.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerGetOneResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.AuthService;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingDetailResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponseForHotel;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingService;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.CheckInOutService;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomTypeDetailResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.services.RoomService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Hotel", description = "APIs liên quan đến quản lý khách sạn")
public class HotelRoomPolicyController {
    private final BookingService bookingService;
    private final RoomService roomService; // Hoặc Service tương ứng của bạn
    private final AuthService authService;
    private final CheckInOutService checkInOutService;

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


    // API lấy danh sách booking theo khách sạn
    @GetMapping("/booking/{hotelId}")
    public ResponseEntity<List<BookingResponseForHotel>> getBookingsByHotel(@PathVariable Long hotelId) {
        List<BookingResponseForHotel> bookings = bookingService.getBookingsByHotel(hotelId);
        return ResponseEntity.ok(bookings);
    }

    /**
     * GET: /bookings/today-checkins
     */
    @GetMapping("/today-checkins")
    @Operation(summary = "Lấy danh sách các phòng dự kiến làm thủ tục Check-in trong ngày hôm nay")
    public ResponseEntity<List<BookingDetailResponse>> getTodayCheckInList(@RequestParam Long hotelId) {
        return ResponseEntity.ok(checkInOutService.getTodayCheckInList(hotelId));
    }

    /**
     * GET: /bookings/today-checkouts
     */
    @GetMapping("/today-checkouts")
    @Operation(summary = "Lấy danh sách các phòng dự kiến làm thủ tục Check-out trong ngày hôm nay")
    public ResponseEntity<List<BookingDetailResponse>> getTodayCheckOutList(@RequestParam Long hotelId) {
        return ResponseEntity.ok(checkInOutService.getTodayCheckOutList(hotelId));
    }


    /**
     * POST: /bookings/{bookingId}/bulk-check-in?employeeId=...
     */
    @PostMapping("/{bookingId}/bulk-check-in")
    @Operation(summary = "Thực hiện thủ tục nhận phòng đồng loạt (Bulk Check-in) cho danh sách phòng được chọn và tính phụ thu sớm")
    public ResponseEntity<BookingResponse> processBulkCheckIn(
            @PathVariable Long bookingId,
            @RequestBody List<Long> bookingDetailIds,
            @RequestParam Long employeeId) {
        BookingResponse response = checkInOutService.processBulkCheckIn(bookingId, bookingDetailIds, employeeId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST: /bookings/{bookingId}/bulk-check-out?employeeId=...
     */
    @PostMapping("/{bookingId}/bulk-check-out")
    @Operation(summary = "Thực hiện thủ tục trả phòng đồng loạt (Bulk Check-out), tính phụ thu lố giờ, chốt tiền dịch vụ và đóng Order CLOSED")
    public ResponseEntity<BookingResponse> processBulkCheckOut(
            @PathVariable Long bookingId,
            @RequestBody List<Long> bookingDetailIds,
            @RequestParam Long employeeId) {
        BookingResponse response = checkInOutService.processBulkCheckOut(bookingId, bookingDetailIds, employeeId);
        return ResponseEntity.ok(response);
    }
}
