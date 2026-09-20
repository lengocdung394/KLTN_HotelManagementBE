package iuh.fit.se.hotelmanagement_be.modular.booking.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingDetailCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingServiceRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.CheckoutSummaryResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Booking", description = "APIs liên quan đến đặt phòng")
public class BookingController {

    BookingService bookingService;

    /**
     * Endpoint 1: Khách hàng tự đặt phòng trực tuyến (Online)
     */
    @PostMapping("/customer")
    @Operation(summary = "Khách hàng tự đặt phòng trực tuyến (Online)")
    public ResponseEntity<BookingResponse> createCustomerBooking(@RequestBody @Valid BookingCreateRequest request) {
        BookingResponse response = bookingService.createCustomerBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint 2: Nhân viên hỗ trợ đặt phòng tại quầy (Offline / Counter)
     */
    @PostMapping("/counter/{employeeId}")
    @Operation(summary = "Nhân viên hỗ trợ đặt phòng tại quầy (Offline / Counter)")
    public ResponseEntity<BookingResponse> createCounterBooking(
            @PathVariable Long employeeId,
            @RequestBody @Valid BookingCreateRequest request) {
        BookingResponse response = bookingService.createCounterBooking(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    /**
     * Endpoint 4: Lấy thông tin tổng quan lúc Checkout (Xem khách còn nợ bao nhiêu tiền dịch vụ)
     */
    @GetMapping("/{bookingId}/checkout-summary")
    @Operation(summary = "Lấy thông tin tổng quan hóa đơn lúc Checkout (Đối soát tiền còn thiếu)")
    public ResponseEntity<CheckoutSummaryResponse> getCheckoutSummary(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getCheckoutSummary(bookingId));
    }




}