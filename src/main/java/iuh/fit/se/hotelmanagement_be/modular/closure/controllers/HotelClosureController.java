package iuh.fit.se.hotelmanagement_be.modular.closure.controllers;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.closure.requests.CreateHotelClosureRequest;
import iuh.fit.se.hotelmanagement_be.modular.closure.requests.ResolveHotelClosureRequest;
import iuh.fit.se.hotelmanagement_be.modular.closure.responses.HotelClosureAffectedBookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.closure.responses.HotelClosureResponse;
import iuh.fit.se.hotelmanagement_be.modular.closure.responses.PublicHotelClosureResponse;
import iuh.fit.se.hotelmanagement_be.modular.closure.services.HotelClosureService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hotel-closures")
@RequiredArgsConstructor
public class HotelClosureController {
    private final HotelClosureService closureService;

    @GetMapping("/public")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<List<PublicHotelClosureResponse>>> publicSchedules(
            @RequestParam Long hotelId) {
        List<PublicHotelClosureResponse> response = closureService.listPublicSchedules(hotelId);
        return ResponseEntity.ok(ApiResponse.<List<PublicHotelClosureResponse>>builder()
                .code(200).message("Lịch tạm đóng khách sạn.")
                .result(response).build());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<HotelClosureResponse>> create(
            @Valid @RequestBody CreateHotelClosureRequest request,
            Authentication authentication) {
        HotelClosureResponse response = closureService.createSchedule(
                request, (Account) authentication.getPrincipal());
        return ResponseEntity.ok(ApiResponse.<HotelClosureResponse>builder()
                .code(200).message("Đã tạo và thông báo lịch tạm đóng khách sạn.")
                .result(response).build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'EMPLOYEE', 'RECEPTIONIST', 'STAFF')")
    public ResponseEntity<ApiResponse<List<HotelClosureResponse>>> list(Authentication authentication) {
        List<HotelClosureResponse> response = closureService.listSchedules(
                (Account) authentication.getPrincipal());
        return ResponseEntity.ok(ApiResponse.<List<HotelClosureResponse>>builder()
                .code(200).message("Danh sách lịch tạm đóng khách sạn.")
                .result(response).build());
    }

    @GetMapping("/{scheduleId}/affected-bookings")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE', 'RECEPTIONIST', 'STAFF')")
    public ResponseEntity<ApiResponse<List<HotelClosureAffectedBookingResponse>>> affectedBookings(
            @PathVariable String scheduleId,
            Authentication authentication) {
        List<HotelClosureAffectedBookingResponse> response = closureService.getAffectedBookings(
                scheduleId, (Account) authentication.getPrincipal());
        return ResponseEntity.ok(ApiResponse.<List<HotelClosureAffectedBookingResponse>>builder()
                .code(200).message("Danh sách booking bị ảnh hưởng.")
                .result(response).build());
    }

    @PostMapping("/{scheduleId}/bookings/{bookingId}/resolve")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE', 'RECEPTIONIST', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> resolve(
            @PathVariable String scheduleId,
            @PathVariable String bookingId,
            @Valid @RequestBody ResolveHotelClosureRequest request,
            Authentication authentication) {
        closureService.resolveBooking(scheduleId, bookingId, request,
                (Account) authentication.getPrincipal());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200).message("Đã ghi nhận hướng xử lý booking.")
                .build());
    }
}
