package iuh.fit.se.hotelmanagement_be.modular.booking.controllers;

import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.impl.BookingServiceImpl;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/booking2s")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingController {

    BookingServiceImpl bookingService;

    /**
     * Endpoint 1: Khách hàng tự đặt phòng trực tuyến (Online)
     * POST: /api/v1/bookings/customer
     */
    @PostMapping("/customer")
    public ResponseEntity<BookingResponse> createCustomerBooking(@RequestBody @Valid BookingCreateRequest request) {
        BookingResponse response = bookingService.createCustomerBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint 2: Nhân viên hỗ trợ đặt phòng tại quầy (Offline / Counter)
     * POST: /api/v1/bookings/counter/{employeeId}
     */
    @PostMapping("/counter/{employeeId}")
    public ResponseEntity<BookingResponse> createCounterBooking(
            @PathVariable Long employeeId,
            @RequestBody @Valid BookingCreateRequest request) {

        BookingResponse response = bookingService.createCounterBooking(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}