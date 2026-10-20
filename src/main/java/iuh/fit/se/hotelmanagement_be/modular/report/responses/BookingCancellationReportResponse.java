package iuh.fit.se.hotelmanagement_be.modular.report.responses;

import java.time.LocalDateTime;

public record BookingCancellationReportResponse(
        String bookingId,
        String bookingChannel,
        String reason,
        LocalDateTime cancelledAt
) {
}
