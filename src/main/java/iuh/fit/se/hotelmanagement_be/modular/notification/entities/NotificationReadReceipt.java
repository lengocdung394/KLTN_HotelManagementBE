package iuh.fit.se.hotelmanagement_be.modular.notification.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_read_receipts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_read_account", columnNames = {"notification_id", "account_id"})
}, indexes = {
        @Index(name = "idx_notification_receipts_account", columnList = "account_id")
})
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationReadReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    Notification notification;

    @Column(name = "account_id", nullable = false, length = 100)
    String accountId;

    @Column(name = "read_at", nullable = false, updatable = false)
    LocalDateTime readAt;

    public NotificationReadReceipt(Notification notification, String accountId) {
        this.notification = notification;
        this.accountId = accountId;
    }

    @PrePersist
    void onCreate() {
        if (readAt == null) readAt = LocalDateTime.now();
    }
}
