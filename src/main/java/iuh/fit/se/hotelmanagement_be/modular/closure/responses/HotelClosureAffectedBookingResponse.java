package iuh.fit.se.hotelmanagement_be.modular.closure.responses;

import java.time.LocalDate;
import java.util.List;

public record HotelClosureAffectedBookingResponse(
        String bookingId,
        String guestName,
        String bookingStatus,
        List<RoomStay> roomStays,
        String resolutionAction) {
    public record RoomStay(
            String bookingDetailId,
            String roomNumber,
            LocalDate checkinDate,
            LocalDate checkoutDate,
            String status) {
    }
}
