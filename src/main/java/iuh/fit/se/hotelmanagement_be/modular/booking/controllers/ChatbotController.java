package iuh.fit.se.hotelmanagement_be.modular.booking.controllers;

import iuh.fit.se.hotelmanagement_be.modular.booking.requests.ChatbotBookingRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.ChatbotBookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.ChatbotBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotBookingService chatbotBookingService;

    @PostMapping("/book")
    public ResponseEntity<ChatbotBookingResponse> createBooking(@RequestBody ChatbotBookingRequest request) {
        return ResponseEntity.ok(chatbotBookingService.createBookingFromChatbot(request));
    }
}
