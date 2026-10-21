package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import java.time.LocalDateTime;

public record HousekeepingTaskResponse(
        String id,
        String roomId,
        String roomNumber,
        String bookingDetailId,
        String status,
        LocalDateTime createdAt,
        LocalDateTime assignedAt
) {
}
