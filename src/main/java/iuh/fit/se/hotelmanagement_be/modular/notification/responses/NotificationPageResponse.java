package iuh.fit.se.hotelmanagement_be.modular.notification.responses;

import java.util.List;

public record NotificationPageResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<NotificationResponse> content
) {
}
