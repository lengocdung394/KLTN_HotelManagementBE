package iuh.fit.se.hotelmanagement_be.modular.chat.entities;

import iuh.fit.se.hotelmanagement_be.modular.chat.enums.ChatSessionStatus;
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
@Table(name = "live_chat_sessions", indexes = {
        @Index(name = "idx_live_chat_session_customer", columnList = "customer_id"),
        @Index(name = "idx_live_chat_session_hotel", columnList = "hotel_id"),
        @Index(name = "idx_live_chat_session_updated", columnList = "updated_at")
})
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "customer_id", nullable = false)
    String customerId;

    @Column(name = "customer_name")
    String customerName;

    @Column(name = "customer_phone")
    String customerPhone;

    @Column(name = "hotel_id")
    Long hotelId;

    @Column(name = "branch_name")
    String branchName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    ChatSessionStatus status = ChatSessionStatus.OPEN;

    @Column(name = "last_message", columnDefinition = "TEXT")
    String lastMessage;

    @Column(name = "last_message_at")
    LocalDateTime lastMessageAt;

    @Column(name = "created_at")
    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
        if (this.lastMessageAt == null) this.lastMessageAt = LocalDateTime.now();
        if (this.status == null) this.status = ChatSessionStatus.OPEN;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
