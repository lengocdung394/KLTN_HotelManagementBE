package iuh.fit.se.hotelmanagement_be.modular.notification.responses;

import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long hotelId,
        NotificationType type,
        String title,
        String description,
        String referenceType,
        String referenceId,
        LocalDateTime createdAt,
        boolean read
) {
}
