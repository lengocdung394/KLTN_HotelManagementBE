package iuh.fit.se.hotelmanagement_be.modular.booking.controllers;


import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingModificationRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.bookingcancel.BookingCancellationRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingModificationResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingServiceResponseForHotel;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.bookingcancel.BookingCancellationResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.bookingcancel.BookingCancellationPreviewResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingManagementService;
import iuh.fit.se.hotelmanagement_be.modular.payment.services.impl.BookingCancellationService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/management-bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Booking", description = "APIs liên quan đến đặt phòng")
public class BookingManagementController {
    BookingManagementService bookingManagementService;
    BookingCancellationService  bookingCancellationService;

    @GetMapping("/{bookingId}/cancellation-preview")
    @PreAuthorize("hasAuthority('UPDATE_BOOKING') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BookingCancellationPreviewResponse>> previewBookingCancellation(
            @PathVariable String bookingId,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        BookingCancellationPreviewResponse result = bookingCancellationService.previewCancellationByStaff(
                bookingId, account);
        return ResponseEntity.ok(ApiResponse.<BookingCancellationPreviewResponse>builder()
                .code(200)
                .message("Đã tính trước khoản hoàn tiền.")
                .result(result)
                .build());
    }

    @PatchMapping("/{bookingId}/service-details/{serviceDetailId}/confirm-use")
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<BookingServiceResponseForHotel>> confirmServiceUsed(
            @PathVariable String bookingId,
            @PathVariable String serviceDetailId,
            Authentication authentication
    ) {
        Account account = (Account) authentication.getPrincipal();
        BookingServiceResponseForHotel result = bookingManagementService.confirmServiceUsed(bookingId, serviceDetailId, account);
        return ResponseEntity.ok(ApiResponse.<BookingServiceResponseForHotel>builder()
                .code(200)
                .message("Đã xác nhận dịch vụ được sử dụng.")
                .result(result)
                .build());
    }

    /**
     * API chỉnh sửa đơn đặt phòng (Hỗ trợ đồng thời: Hủy phòng, thêm phòng, đổi phòng, đổi ngày, hủy dịch vụ).
     *
     * @param bookingId ID của đơn đặt phòng cần thay đổi.
     * @param request   DTO chứa danh sách các thay đổi.
     * @return Thông báo thành công kèm theo request gốc hoặc kết quả trả về.
     */
    @PreAuthorize("hasAuthority('UPDATE_BOOKING')")
    @PutMapping("/{bookingId}/modify")
    public ResponseEntity<ApiResponse<BookingModificationResponse>> modifyBooking(
            @PathVariable String bookingId,
            @RequestBody BookingModificationRequest request
    ) {
        log.info("Received request to modify booking ID: {}", bookingId);

        BookingModificationResponse result = bookingManagementService.modifyBooking(bookingId, request);

        return ResponseEntity.ok(
                ApiResponse.<BookingModificationResponse>builder()
                        .code(200)
                        .message("Booking modified successfully")
                        .result(result)
                        .build()
        );
    }

    @PostMapping("/{bookingId}/cancel")
    @PreAuthorize("hasAuthority('UPDATE_BOOKING') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BookingCancellationResponse>> cancelBooking(
            @PathVariable String bookingId,
            @Valid @RequestBody(required = false) BookingCancellationRequest request,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        BookingCancellationResponse result = bookingCancellationService.requestCancellationByStaff(
                bookingId, request, account);
        return ResponseEntity.ok(ApiResponse.<BookingCancellationResponse>builder()
                .code(1000)
                .message("Booking đã được nhân viên hủy; yêu cầu hoàn tiền sẽ chờ thông tin ngân hàng nếu cần.")
                .result(result)
                .build());
    }

}
