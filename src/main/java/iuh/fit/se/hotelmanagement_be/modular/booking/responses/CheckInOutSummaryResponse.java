package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

public record CheckInOutSummaryResponse(
        long waitingCheckIn,
        long inHouse,
        long waitingCheckOut,
        long completed
) {
}
