package iuh.fit.se.hotelmanagement_be.modular.chat.entities;

import iuh.fit.se.hotelmanagement_be.modular.chat.enums.ChatSenderType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "live_chat_messages", indexes = {
        @Index(name = "idx_live_chat_msg_session", columnList = "session_id"),
        @Index(name = "idx_live_chat_msg_created_at", columnList = "created_at"),
        @Index(name = "idx_live_chat_msg_id", columnList = "msg_id")
})
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    ChatSenderType senderType;

    @Column(name = "sender_id")
    String senderId;

    @Column(name = "sender_name")
    String senderName;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    String message;

    @Column(name = "msg_id")
    String msgId;

    @Column(name = "is_read")
    @Builder.Default
    Boolean isRead = false;

    @Column(name = "created_at")
    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.isRead == null) this.isRead = false;
    }
}
