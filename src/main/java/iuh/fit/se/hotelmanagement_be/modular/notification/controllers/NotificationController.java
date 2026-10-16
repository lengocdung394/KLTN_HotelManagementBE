package iuh.fit.se.hotelmanagement_be.modular.notification.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import iuh.fit.se.hotelmanagement_be.modular.notification.responses.NotificationPageResponse;
import iuh.fit.se.hotelmanagement_be.modular.notification.responses.NotificationResponse;
import iuh.fit.se.hotelmanagement_be.modular.notification.responses.NotificationSummaryResponse;
import iuh.fit.se.hotelmanagement_be.modular.notification.services.NotificationService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Thông báo realtime, trạng thái đã đọc và thống kê theo chi nhánh")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Lấy danh sách thông báo trong phạm vi tài khoản")
    public ResponseEntity<ApiResponse<NotificationPageResponse>> getNotifications(
            Authentication authentication,
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Account account = (Account) authentication.getPrincipal();
        NotificationPageResponse result = notificationService.getNotifications(
                account, hotelId, type, unreadOnly, page, size);
        return ResponseEntity.ok(ApiResponse.<NotificationPageResponse>builder()
                .code(200)
                .message("Lấy danh sách thông báo thành công")
                .result(result)
                .build());
    }

    @GetMapping("/summary")
    @Operation(summary = "Thống kê số thông báo và số chưa đọc theo loại/chi nhánh")
    public ResponseEntity<ApiResponse<NotificationSummaryResponse>> getSummary(
            Authentication authentication,
            @RequestParam(required = false) Long hotelId) {
        Account account = (Account) authentication.getPrincipal();
        NotificationSummaryResponse result = notificationService.getSummary(account, hotelId);
        return ResponseEntity.ok(ApiResponse.<NotificationSummaryResponse>builder()
                .code(200)
                .message("Lấy thống kê thông báo thành công")
                .result(result)
                .build());
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Đánh dấu một thông báo đã đọc")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            Authentication authentication,
            @PathVariable Long id) {
        Account account = (Account) authentication.getPrincipal();
        NotificationResponse result = notificationService.markAsRead(account, id);
        return ResponseEntity.ok(ApiResponse.<NotificationResponse>builder()
                .code(200)
                .message("Đã đánh dấu thông báo là đã đọc")
                .result(result)
                .build());
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Đánh dấu các thông báo trong phạm vi hiện tại đã đọc")
    public ResponseEntity<ApiResponse<Long>> markAllAsRead(
            Authentication authentication,
            @RequestParam(required = false) Long hotelId) {
        Account account = (Account) authentication.getPrincipal();
        long count = notificationService.markAllAsRead(account, hotelId);
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .code(200)
                .message("Đã đánh dấu tất cả thông báo là đã đọc")
                .result(count)
                .build());
    }
}

