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
@Tag(name = "Booking", description = "APIs liÃªn quan Ä‘áº¿n Ä‘ÄƒÌ£t phoÌ€ng")
public class BookingController {

    BookingService bookingService;
    iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository roomRepository;

    @GetMapping("/available-rooms")
    public ResponseEntity<?> getAvailableRooms() {
        return ResponseEntity.ok(roomRepository.findAll());
    }

    /**
     * Endpoint 1: KhÃ¡ch hÃ ng tá»± Ä‘áº·t phÃ²ng trá»±c tuyáº¿n (Online)
     */
    @PostMapping("/customer")
    @Operation(summary = "KhÃ¡ch hÃ ng tá»± Ä‘áº·t phÃ²ng trá»±c tuyáº¿n (Online)")
    public ResponseEntity<BookingResponse> createCustomerBooking(@RequestBody @Valid BookingCreateRequest request) {
        BookingResponse response = bookingService.createCustomerBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('CREATE_BOOKING')")
    @PostMapping("/counter")
    @Operation(summary = "NhÃ¢n viÃªn há»— trá»£ Ä‘áº·t phÃ²ng táº¡i quáº§y")
    public ResponseEntity<BookingResponse> createCounterBooking(
            @RequestBody @Valid BookingCreateRequest request,
            Authentication authentication
    ) {
        // 1. Láº¥y thÃ´ng tin tÃ i khoáº£n Ä‘ang Ä‘Äƒng nháº­p tá»« token
        Account account = (Account) authentication.getPrincipal();

        // 2. Láº¥y mÃ£ nhÃ¢n viÃªn vÃ  mÃ£ khÃ¡ch sáº¡n trá»±c tiáº¿p ngáº§m tá»« token
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
    @Operation(summary = "Láº¥y thÃ´ng tin tá»•ng quan hÃ³a Ä‘Æ¡n lÃºc Checkout (Äá»‘i soÃ¡t tiá»n cÃ²n thiáº¿u)")
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