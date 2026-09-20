package iuh.fit.se.hotelmanagement_be.modular.booking.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingDetailCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingServiceRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingManagementService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/management-bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Booking", description = "APIs liên quan đến đặt phòng")
public class BookingManagementController {
    BookingManagementService bookingManagementService;
//
//    /**
//     * Endpoint 5: Hủy bớt 1 phòng hoặc nhiều phòng cùng lúc trong Booking lớn
//     */
//    @PostMapping("/{bookingId}/cancel-rooms")
//    @Operation(summary = "Hủy một hoặc nhiều phòng cụ thể trong đơn đặt phòng lớn")
//    public ResponseEntity<BookingResponse> cancelRooms(
//            @PathVariable Long bookingId,
//            @RequestBody List<Long> bookingDetailIds,
//            @RequestParam Long employeeId) {
//        BookingResponse response = bookingManagementService.cancelRooms(bookingId, bookingDetailIds, employeeId);
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * Endpoint 6: Hủy toàn bộ đơn đặt phòng (Hủy sạch sẽ cả cái Booking)
//     */
//    @PostMapping("/{bookingId}/cancel-all")
//    @Operation(summary = "Hủy bỏ toàn bộ đơn đặt phòng lớn (Hủy sạch các phòng)")
//    public ResponseEntity<BookingResponse> cancelEntireBooking(
//            @PathVariable Long bookingId,
//            @RequestParam Long employeeId) {
//        BookingResponse response = bookingManagementService.cancelEntireBooking(bookingId, employeeId);
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * Endpoint 7: Khách yêu cầu đặt thêm phòng phát sinh gối đầu (Lúc Check-in tại quầy)
//     */
//    @PostMapping("/{bookingId}/add-rooms")
//    @Operation(summary = "Đặt thêm một hoặc nhiều phòng phát sinh gối đầu vào Booking hiện tại")
//    public ResponseEntity<BookingResponse> addRoomToExistingBooking(
//            @PathVariable Long bookingId,
//            @RequestBody List<BookingDetailCreateRequest> additionalRoomRequests) {
//        BookingResponse response = bookingManagementService.addRoomToExistingBooking(bookingId, additionalRoomRequests);
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * Endpoint 8: Chỉnh sửa số lượng hoặc hủy bớt dịch vụ đi kèm của 1 phòng (Dùng linh hoạt lúc ở phòng hoặc Checkout)
//     */
//    @PutMapping("/{bookingId}/details/{bookingDetailId}/services")
//    @Operation(summary = "Chỉnh sửa số lượng hoặc hủy bớt dịch vụ của một phòng cụ thể (Hỗ trợ Check-in/Check-out)")
//    public ResponseEntity<BookingResponse> updateOrCancelServices(
//            @PathVariable Long bookingId,
//            @PathVariable Long bookingDetailId,
//            @RequestBody List<BookingServiceRequest> updatedServiceRequests,
//            @RequestParam Long employeeId) {
//        BookingResponse response = bookingManagementService.updateOrCancelServices(bookingId, bookingDetailId, updatedServiceRequests, employeeId);
//        return ResponseEntity.ok(response);
//    }
//
//
//    /**
//     * Endpoint 3: Đang ở phòng, gọi thêm dịch vụ phát sinh (Tính vào tiền checkout)
//     */
//    @PostMapping("/{bookingId}/services")
//    @Operation(summary = "Gọi thêm dịch vụ phát sinh trong quá trình lưu trú (Room Charge)")
//    public ResponseEntity<BookingResponse> addServicesToBooking(
//            @PathVariable Long bookingId,
//            @RequestBody List<BookingServiceRequest> serviceRequests) {
//        return ResponseEntity.ok(bookingManagementService.addServiceToExistingBooking(bookingId, serviceRequests));
//    }
}
