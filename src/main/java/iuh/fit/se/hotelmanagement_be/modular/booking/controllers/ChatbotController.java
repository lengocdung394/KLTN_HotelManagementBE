package iuh.fit.se.hotelmanagement_be.modular.booking.controllers;

import iuh.fit.se.hotelmanagement_be.modular.booking.requests.ChatbotBookingRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.ChatbotBookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.ChatbotBookingService;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.bookingcancel.BookingCancellationResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.services.impl.BookingCancellationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/internal/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotBookingService chatbotBookingService;
    private final BookingCancellationService bookingCancellationService;

    @PostMapping("/book")
    public ResponseEntity<ChatbotBookingResponse> createBooking(@RequestBody ChatbotBookingRequest request) {
        return ResponseEntity.ok(chatbotBookingService.createBookingFromChatbot(request));
    }

    @PostMapping("/cancel")
    public ResponseEntity<BookingCancellationResponse> cancelBooking(@RequestBody Map<String, String> payload) {
        String bookingId = payload.getOrDefault("bookingId", "").trim();
        String reason = payload.getOrDefault("reason", "Khách hàng yêu cầu hủy qua Trợ lý AI");
        String customerEmail = payload.get("customerEmail");
        String customerPhone = payload.get("customerPhone");
        return ResponseEntity.ok(
                bookingCancellationService.cancelBookingFromChatbot(bookingId, reason, customerEmail, customerPhone)
        );
    }
}
