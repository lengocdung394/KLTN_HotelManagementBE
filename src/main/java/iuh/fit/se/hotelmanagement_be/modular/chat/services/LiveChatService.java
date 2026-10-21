package iuh.fit.se.hotelmanagement_be.modular.chat.services;

import iuh.fit.se.hotelmanagement_be.modular.chat.responses.ChatMessageResponse;
import iuh.fit.se.hotelmanagement_be.modular.chat.responses.ChatSessionResponse;

import java.util.List;

public interface LiveChatService {
    ChatMessageResponse saveCustomerMessage(
            String customerId,
            String customerName,
            String customerPhone,
            Long hotelId,
            String branchName,
            String message,
            String msgId
    );

    ChatMessageResponse saveStaffMessage(
            String customerId,
            String staffId,
            String staffName,
            Long hotelId,
            String branchName,
            String message,
            String msgId
    );

    List<ChatMessageResponse> getMessagesByCustomerId(String customerId);

    List<ChatSessionResponse> getAllSessions(Long hotelId);

    void closeSession(Long sessionId);
}
