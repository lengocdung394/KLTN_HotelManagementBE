package iuh.fit.se.hotelmanagement_be.modular.payment.controllers;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingCancellationStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.bookingcancel.BookingCancellationRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.bookingcancel.BookingCancellationReviewRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.bookingcancel.BookingCancellationResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.services.impl.BookingCancellationService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/management-bookings/cancellations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasAuthority('UPDATE_BOOKING') or hasRole('SUPER_ADMIN')")
public class BookingCancellationManagementController {
    BookingCancellationService bookingCancellationService;


    // xem ds booking can toan tien
    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingCancellationResponse>>> getCancellations(
            @RequestParam(required = false) BookingCancellationStatus status,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<List<BookingCancellationResponse>>builder()
                .code(1000)
                .result(bookingCancellationService.getCancellations(status, account))
                .build());
    }

    // Nhân viên bấm yêu cầu hủy booking
    @PatchMapping("/{cancellationId}/approve")
    public ResponseEntity<ApiResponse<BookingCancellationResponse>> approveRefund(
            @PathVariable String cancellationId,
            @Valid @RequestBody(required = false) BookingCancellationReviewRequest request,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<BookingCancellationResponse>builder()
                .code(1000)
                .message("Đã duyệt hoàn tiền. Quản lý có thể chuyển khoản đến tài khoản khách.")
                .result(bookingCancellationService.approveRefund(cancellationId, request, account))
                .build());
    }

    // yeu cau hoan tien bi tu choi
    @PatchMapping("/{cancellationId}/reject")
    public ResponseEntity<ApiResponse<BookingCancellationResponse>> rejectRefund(
            @PathVariable String cancellationId,
            @Valid @RequestBody BookingCancellationReviewRequest request,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<BookingCancellationResponse>builder()
                .code(1000)
                .message("Đã từ chối yêu cầu hoàn tiền.")
                .result(bookingCancellationService.rejectRefund(cancellationId, request, account))
                .build());
    }

    // xac nhan hoan tien
    @PatchMapping("/{cancellationId}/confirm-refund")
    public ResponseEntity<ApiResponse<BookingCancellationResponse>> confirmRefund(
            @PathVariable String cancellationId,
            @Valid @RequestBody(required = false) BookingCancellationReviewRequest request,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<BookingCancellationResponse>builder()
                .code(1000)
                .message("Đã ghi nhận chuyển khoản hoàn tiền.")
                .result(bookingCancellationService.confirmRefund(cancellationId, request, account))
                .build());
    }
    // cap nhat thong tin tai khoản cho khách hàng
    @PatchMapping("/{cancellationId}/bank-details")
    public ResponseEntity<ApiResponse<BookingCancellationResponse>> submitRefundBankDetails(
            @PathVariable String cancellationId,
            @Valid @RequestBody BookingCancellationRequest request,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<BookingCancellationResponse>builder()
                .code(1000)
                .message("Đã lưu thông tin tài khoản nhận hoàn; yêu cầu đang chờ quản lý duyệt.")
                .result(bookingCancellationService.submitRefundBankDetailsByStaff(cancellationId, request, account))
                .build());
    }
}
