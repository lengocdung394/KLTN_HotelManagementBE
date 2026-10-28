package iuh.fit.se.hotelmanagement_be.modular.chat.responses;

import com.fasterxml.jackson.annotation.JsonFormat;
import iuh.fit.se.hotelmanagement_be.modular.chat.enums.ChatSenderType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessageResponse {
    Long id;
    Long sessionId;
    ChatSenderType senderType;
    String senderId;
    String senderName;
    String message;
    String msgId;
    Boolean isRead;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt;
}
