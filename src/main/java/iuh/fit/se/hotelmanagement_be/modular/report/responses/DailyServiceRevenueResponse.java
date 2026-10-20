package iuh.fit.se.hotelmanagement_be.modular.report.responses;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyServiceRevenueResponse(
        LocalDate date,
        BigDecimal revenue,
        long closedInvoiceCount
) {
}
