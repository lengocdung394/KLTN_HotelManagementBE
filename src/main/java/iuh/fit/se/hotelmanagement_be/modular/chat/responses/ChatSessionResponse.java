package iuh.fit.se.hotelmanagement_be.modular.chat.responses;

import com.fasterxml.jackson.annotation.JsonFormat;
import iuh.fit.se.hotelmanagement_be.modular.chat.enums.ChatSessionStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatSessionResponse {
    Long id;
    String customerId;
    String customerName;
    String customerPhone;
    Long hotelId;
    String branchName;
    ChatSessionStatus status;
    String lastMessage;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime lastMessageAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt;
    List<ChatMessageResponse> messages;
}
