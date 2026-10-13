package iuh.fit.se.hotelmanagement_be.modular.booking.services;

import iuh.fit.se.hotelmanagement_be.modular.booking.requests.ChatbotBookingRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.ChatbotBookingResponse;

public interface ChatbotBookingService {
    ChatbotBookingResponse createBookingFromChatbot(ChatbotBookingRequest request);
}
