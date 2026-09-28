package iuh.fit.se.hotelmanagement_be.modular.ai.services;

import iuh.fit.se.hotelmanagement_be.modular.ai.requests.ChatRequest;
import iuh.fit.se.hotelmanagement_be.modular.ai.responses.ChatResponse;

public interface AiChatService {
    ChatResponse chat(ChatRequest request);
}
