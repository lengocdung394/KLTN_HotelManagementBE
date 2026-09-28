package iuh.fit.se.hotelmanagement_be.modular.ai.requests;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatRequest {
    String message;
    String sessionId;
    List<ChatMessage> history;
}
