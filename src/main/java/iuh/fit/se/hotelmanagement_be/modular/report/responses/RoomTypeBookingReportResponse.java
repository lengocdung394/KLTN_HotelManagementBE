package iuh.fit.se.hotelmanagement_be.modular.report.responses;

public record RoomTypeBookingReportResponse(
        String roomType,
        long bookingCount
) {
}
