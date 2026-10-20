package iuh.fit.se.hotelmanagement_be.modular.report.responses;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record HotelReportResponse(
        LocalDate startDate,
        LocalDate endDate,
        long totalBookingCount,
        BigDecimal cashReceived,
        BigDecimal cashRefunded,
        BigDecimal cashChangeReturned,
        BigDecimal netCashReceived,
        BigDecimal serviceRevenue,
        long closedInvoiceCount,
        List<DailyServiceRevenueResponse> dailyServiceRevenue,
        long promotionUsageCount,
        BigDecimal promotionSavings,
        List<PromotionUsageReportResponse> promotions,
        long cancelledBookingCount,
        long onlineCancelledBookingCount,
        long offlineCancelledBookingCount,
        List<BookingCancellationReportResponse> cancelledBookings,
        long totalRoomCount,
        List<RoomStatusReportResponse> roomStatuses
) {
}
