package iuh.fit.se.hotelmanagement_be.modular.booking.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.CheckoutSummaryResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.RoomMatrixResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Booking", description = "APIs liên quan đến đặt phòng")
public class BookingController {

    BookingService bookingService;
    iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository roomRepository;
    iuh.fit.se.hotelmanagement_be.modular.room.services.RoomService roomService;

    @GetMapping("/available-rooms")
    public ResponseEntity<?> getAvailableRooms(@RequestParam(required = false) Long hotelId) {
        if (hotelId != null) {
            return ResponseEntity.ok(roomService.getRoomsByHotelId(hotelId));
        }
        return ResponseEntity.ok(roomRepository.findAll().stream().map(r -> java.util.Map.of(
                "roomId", r.getId() != null ? r.getId() : "",
                "roomType", r.getRoomType() != null ? r.getRoomType().name() : "",
                "roomStatus", r.getRoomStatus() != null ? r.getRoomStatus().name() : "",
                "basePrice", r.getBasePrice() != null ? r.getBasePrice() : 0.0
        )).toList());
    }

    /**
     * Endpoint 1: Khách hàng tự đặt phòng trực tuyến (Online)
     */
    @PostMapping("/customer")
    @Operation(summary = "Khách hàng tự đặt phòng trực tuyến (Online)")
    public ResponseEntity<BookingResponse> createCustomerBooking(@RequestBody @Valid BookingCreateRequest request) {
        BookingResponse response = bookingService.createCustomerBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('CREATE_BOOKING')")
    @PostMapping("/counter")
    @Operation(summary = "Nhân viên hỗ trợ đặt phòng tại quầy")
    public ResponseEntity<BookingResponse> createCounterBooking(
            @RequestBody @Valid BookingCreateRequest request,
            Authentication authentication
    ) {
        // 1. Láº¥y thÃ´ng tin tÃ i khoáº£n Ä‘ang Ä‘Äƒng nháº­p tá»« token
        Account account = (Account) authentication.getPrincipal();

        // 2. Lấy mã nhân viên và mã khách sạn trực tiếp ngầm từ token
        String employeeId = account.getEmployeeId();
        Long hotelId = account.getHotelId();

        // 3. Truyá»n xuá»‘ng service
        BookingResponse response = bookingService.createCounterBooking(employeeId, hotelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    /**
     * Endpoint 4: Láº¥y thÃ´ng tin tá»•ng quan lÃºc Checkout (Xem khÃ¡ch cÃ²n ná»£ bao nhiÃªu tiá»n dá»‹ch vá»¥)
     */
    @GetMapping("/{bookingId}/checkout-summary")
    @Operation(summary = "Lấy thông tin tổng quan hóa đơn lúc Checkout (Đối soát tiền còn thiếu)")
    public ResponseEntity<CheckoutSummaryResponse> getCheckoutSummary(@PathVariable String bookingId) {
        return ResponseEntity.ok(bookingService.getCheckoutSummary(bookingId));
    }




    @Operation(summary = "Lấy danh sách các đơn đặt phòng của khách hàng đang đăng nhập")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my")
    public ResponseEntity<iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse<List<BookingResponse>>> getMyBookings(org.springframework.security.core.Authentication authentication) {
        iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account account = (authentication != null && authentication.getPrincipal() instanceof iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account a) ? a : null;
        List<BookingResponse> responses = bookingService.getMyBookings(account);
        return ResponseEntity.ok(iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse.<List<BookingResponse>>builder()
                .code(1000)
                .message("Lấy danh sách đơn đặt phòng thành công")
                .result(responses)
                .build());
    }
}