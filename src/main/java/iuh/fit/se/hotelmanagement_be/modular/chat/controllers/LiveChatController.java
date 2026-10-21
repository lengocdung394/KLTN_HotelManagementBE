package iuh.fit.se.hotelmanagement_be.modular.chat.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.chat.responses.ChatMessageResponse;
import iuh.fit.se.hotelmanagement_be.modular.chat.responses.ChatSessionResponse;
import iuh.fit.se.hotelmanagement_be.modular.chat.services.LiveChatService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/live-chat")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Live Chat", description = "APIs quản lý tin nhắn và phiên chat trực tiếp giữa khách hàng và nhân viên")
public class LiveChatController {

    LiveChatService liveChatService;

    @GetMapping("/messages")
    @Operation(summary = "Lấy lịch sử tin nhắn của khách hàng", description = "Truy xuất danh sách tin nhắn theo customerId để khôi phục khi F5 hoặc đổi thiết bị")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @RequestParam String customerId
    ) {
        List<ChatMessageResponse> messages = liveChatService.getMessagesByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.<List<ChatMessageResponse>>builder()
                .code(1000)
                .message("Lấy lịch sử tin nhắn thành công")
                .result(messages)
                .build());
    }

    @GetMapping("/sessions")
    @Operation(summary = "Lấy danh sách các cuộc trò chuyện", description = "Dành cho nhân viên lễ tân/quản trị viên xem tất cả phiên chat của các khách hàng")
    public ResponseEntity<ApiResponse<List<ChatSessionResponse>>> getSessions(
            @RequestParam(required = false) Long hotelId
    ) {
        List<ChatSessionResponse> sessions = liveChatService.getAllSessions(hotelId);
        return ResponseEntity.ok(ApiResponse.<List<ChatSessionResponse>>builder()
                .code(1000)
                .message("Lấy danh sách phiên chat thành công")
                .result(sessions)
                .build());
    }

    @PatchMapping("/sessions/{id}/close")
    @Operation(summary = "Đóng cuộc trò chuyện", description = "Đánh dấu cuộc trò chuyện đã hoàn tất")
    public ResponseEntity<ApiResponse<Void>> closeSession(@PathVariable Long id) {
        liveChatService.closeSession(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(1000)
                .message("Đã đóng phiên chat thành công")
                .build());
    }
}
