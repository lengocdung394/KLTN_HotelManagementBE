package iuh.fit.se.hotelmanagement_be.modular.report.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundActivityReportResponse(
        String bookingId,
        BigDecimal amount,
        String paymentMethod,
        String status,
        LocalDateTime requestedAt,
        LocalDateTime refundedAt
) {
}
