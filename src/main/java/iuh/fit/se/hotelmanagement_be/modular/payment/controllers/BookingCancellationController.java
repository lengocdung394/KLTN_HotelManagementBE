package iuh.fit.se.hotelmanagement_be.modular.payment.controllers;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.bookingcancel.BookingCancellationRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.bookingcancel.BookingCancellationResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.services.impl.BookingCancellationService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingCancellationController {
    BookingCancellationService bookingCancellationService;

    // yeu cau huy booking
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingCancellationResponse>> requestCancellation(
            @PathVariable String bookingId,
            @Valid @RequestBody(required = false) BookingCancellationRequest request,
            Authentication authentication) {
        Account account = (authentication != null && authentication.getPrincipal() instanceof Account a)
                ? a
                : null;
        BookingCancellationResponse result = bookingCancellationService.requestCancellation(
                bookingId, request, account);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BookingCancellationResponse>builder()
                        .code(1000)
                        .message("Booking đã hủy; yêu cầu hoàn tiền đang chờ quản lý xử lý nếu có.")
                        .result(result)
                        .build());
    }

    @PutMapping("/{bookingId}/cancel/bank-details")
    public ResponseEntity<ApiResponse<BookingCancellationResponse>> submitRefundBankDetails(
            @PathVariable String bookingId,
            @Valid @RequestBody BookingCancellationRequest request,
            Authentication authentication) {
        Account account = (authentication != null && authentication.getPrincipal() instanceof Account a)
                ? a
                : null;
        BookingCancellationResponse result = bookingCancellationService.submitRefundBankDetails(
                bookingId, request, account);
        return ResponseEntity.ok(ApiResponse.<BookingCancellationResponse>builder()
                .code(1000)
                .message("Đã cập nhật thông tin tài khoản nhận hoàn tiền.")
                .result(result)
                .build());
    }
}
