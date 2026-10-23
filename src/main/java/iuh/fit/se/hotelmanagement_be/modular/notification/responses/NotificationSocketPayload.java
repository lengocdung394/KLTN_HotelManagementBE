package iuh.fit.se.hotelmanagement_be.modular.notification.responses;

import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;

public record NotificationSocketPayload(
        Long id,
        Long hotelId,
        NotificationType type,
        String title,
        String description,
        String referenceType,
        String referenceId,
        String createdAt
) {
}
