package iuh.fit.se.hotelmanagement_be.modular.report.responses;

public record RoomStatusReportResponse(
        String status,
        long roomCount
) {
}
