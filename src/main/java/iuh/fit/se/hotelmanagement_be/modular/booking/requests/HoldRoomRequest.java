package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import java.time.LocalDateTime;

public record HoldRoomRequest(
        String roomId,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime
) {}
