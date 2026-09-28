package iuh.fit.se.hotelmanagement_be.modular.ai.controllers;

import iuh.fit.se.hotelmanagement_be.modular.ai.requests.ChatRequest;
import iuh.fit.se.hotelmanagement_be.modular.ai.responses.ChatResponse;
import iuh.fit.se.hotelmanagement_be.modular.ai.services.AiChatService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "AI Chatbot", description = "Trợ lý AI tư vấn đặt phòng Sen Việt")
public class AiChatController {

    AiChatService aiChatService;

    @PostMapping("/chat")
    @Operation(summary = "Gửi tin nhắn cho AI Chatbot", description = "Nhận câu hỏi từ khách hàng, trả lời bằng AI dựa trên dữ liệu khách sạn thời gian thực")
    public ApiResponse<ChatResponse> chat(@RequestBody ChatRequest request) {
        ChatResponse response = aiChatService.chat(request);
        return ApiResponse.<ChatResponse>builder()
                .code(1000)
                .message("Success")
                .result(response)
                .build();
    }
}
