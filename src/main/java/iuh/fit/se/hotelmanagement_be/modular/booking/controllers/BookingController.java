package iuh.fit.se.hotelmanagement_be.modular.booking.controllers;

import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingController {

    BookingService bookingService;


    @PostMapping("/online")
    public ResponseEntity<BookingResponse> createOnlineBooking(
            @RequestBody @Valid BookingCreateRequest request
    ) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/counter")
    public ResponseEntity<BookingResponse> createCounterBooking(
            @RequestParam Long employeeId,
            @RequestBody @Valid BookingCreateRequest request
    ) {
        BookingResponse response = bookingService.createCounterBooking(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}