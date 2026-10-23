package iuh.fit.se.hotelmanagement_be.modular.room.requests;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus;
import jakarta.validation.constraints.NotNull;

public record RoomStatusUpdateRequest(@NotNull RoomStatus roomStatus) {
}
