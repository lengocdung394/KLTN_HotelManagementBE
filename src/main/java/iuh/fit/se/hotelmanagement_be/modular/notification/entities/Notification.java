package iuh.fit.se.hotelmanagement_be.modular.notification.entities;

import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_hotel_created", columnList = "hotel_id, created_at"),
        @Index(name = "idx_notifications_type_created", columnList = "type, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "hotel_id")
    Long hotelId;

    @Column(name = "recipient_account_id", length = 100)
    String recipientAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    NotificationType type;

    @Column(nullable = false, length = 200)
    String title;

    @Column(nullable = false, length = 2000)
    String description;

    @Column(name = "reference_type", length = 40)
    String referenceType;

    @Column(name = "reference_id", length = 100)
    String referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
    @Column(name = "visible_to_hotel", nullable = false)
    boolean visibleToHotel = true;

    @Column(name = "visible_to_super_admin", nullable = false)
    boolean visibleToSuperAdmin = true;
    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
