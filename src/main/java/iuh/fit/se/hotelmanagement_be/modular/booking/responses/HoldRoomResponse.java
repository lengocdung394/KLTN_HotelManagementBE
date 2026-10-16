package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import java.time.Instant;
import java.time.LocalDate;

public record HoldRoomResponse(
        String lockToken,
        String roomId,
        LocalDate checkIn,
        LocalDate checkOut,
        Instant expiresAt
) {}
