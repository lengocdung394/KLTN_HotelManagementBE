package iuh.fit.se.hotelmanagement_be.modular.payment.controllers;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingCancellationStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.bookingcancel.BookingCancellationRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.bookingcancel.BookingCancellationReviewRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.bookingcancel.BookingCancellationResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.bookingcancel.RefundBankDetailsResponse;
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
public class BookingCancellationManagementController {
    BookingCancellationService bookingCancellationService;

    // Xem danh sách booking cần hoàn tiền: Lễ tân (STAFF/RECEPTIONIST), Quản lý (MANAGER), Admin đều được xem
    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'STAFF', 'EMPLOYEE', 'MANAGER', 'ADMIN', 'SUPER_ADMIN') or hasAuthority('UPDATE_BOOKING') or hasAuthority('VIEW_BOOKING')")
    public ResponseEntity<ApiResponse<List<BookingCancellationResponse>>> getCancellations(
            @RequestParam(required = false) BookingCancellationStatus status,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<List<BookingCancellationResponse>>builder()
                .code(1000)
                .result(bookingCancellationService.getCancellations(status, account))
                .build());
    }

    // Chỉ Quản lý (MANAGER) hoặc ADMIN/SUPER_ADMIN mới có quyền bấm phê duyệt hoàn tiền
    @PatchMapping("/{cancellationId}/approve")
    @PreAuthorize("hasRole('MANAGER') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BookingCancellationResponse>> approveRefund(
            @PathVariable String cancellationId,
            @Valid @RequestBody(required = false) BookingCancellationReviewRequest request,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<BookingCancellationResponse>builder()
                .code(1000)
                .message("Đã duyệt hoàn tiền sau khi xác nhận số tiền và tài khoản nhận.")
                .result(bookingCancellationService.approveRefund(cancellationId, request, account))
                .build());
    }

    // Yêu cầu hoàn tiền bị từ chối: Chỉ Quản lý / Admin
    @PatchMapping("/{cancellationId}/reject")
    @PreAuthorize("hasRole('MANAGER') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
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

    // Gửi payout qua PayOS sau khi nhân viên đã duyệt thông tin hoàn tiền
    @PostMapping("/{cancellationId}/payout")
    @PreAuthorize("hasRole('MANAGER') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BookingCancellationResponse>> requestPayout(
            @PathVariable String cancellationId,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<BookingCancellationResponse>builder()
                .code(1000)
                .message("Đã gửi hoặc cập nhật trạng thái payout từ PayOS.")
                .result(bookingCancellationService.requestPayout(cancellationId, account))
                .build());
    }

    // Tương thích API cũ; thao tác này hiện khởi chạy payout PayOS, không tự ghi nhận hoàn tất.
    @PatchMapping("/{cancellationId}/confirm-refund")
    @PreAuthorize("hasRole('MANAGER') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BookingCancellationResponse>> confirmRefund(
            @PathVariable String cancellationId,
            @Valid @RequestBody(required = false) BookingCancellationReviewRequest request,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<BookingCancellationResponse>builder()
                .code(1000)
                .message("Đã gửi hoặc cập nhật trạng thái payout từ PayOS.")
                .result(bookingCancellationService.confirmRefund(cancellationId, request, account))
                .build());
    }

    @GetMapping("/{cancellationId}/bank-details")
    @PreAuthorize("hasRole('MANAGER') or hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundBankDetailsResponse>> getRefundBankDetails(
            @PathVariable String cancellationId,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<RefundBankDetailsResponse>builder()
                .code(1000)
                .result(bookingCancellationService.getRefundBankDetails(cancellationId, account))
                .build());
    }

    // Nhân viên lễ tân có thể hỗ trợ nhập / cập nhật thông tin tài khoản ngân hàng cho khách
    @PatchMapping("/{cancellationId}/bank-details")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'STAFF', 'EMPLOYEE', 'MANAGER', 'ADMIN', 'SUPER_ADMIN') or hasAuthority('UPDATE_BOOKING')")
    public ResponseEntity<ApiResponse<BookingCancellationResponse>> submitRefundBankDetails(
            @PathVariable String cancellationId,
            @Valid @RequestBody BookingCancellationRequest request,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<BookingCancellationResponse>builder()
                .code(1000)
                .message("Đã lưu/cập nhật tài khoản nhận hoàn.")
                .result(bookingCancellationService.submitRefundBankDetailsByStaff(cancellationId, request, account))
                .build());
    }
}
