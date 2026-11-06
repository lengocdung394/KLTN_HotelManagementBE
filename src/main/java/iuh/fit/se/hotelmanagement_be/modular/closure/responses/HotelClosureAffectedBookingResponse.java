package iuh.fit.se.hotelmanagement_be.modular.closure.responses;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record HotelClosureAffectedBookingResponse(
        String bookingId,
        String guestName,
        String bookingStatus,
        List<RoomStay> roomStays,
        String resolutionAction,
        BigDecimal cancellationRefundAmount,
        BigDecimal cancellationRoomRefundAmount,
        BigDecimal cancellationServiceRefundAmount,
        BigDecimal cancellationRoomPenaltyAmount,
        String cancellationRefundPolicy,
        BigDecimal earlyCheckoutRefundAmount,
        BigDecimal earlyCheckoutOriginalRoomAmount,
        int earlyCheckoutOriginalRoomNights,
        int earlyCheckoutUsedRoomNights,
        BigDecimal earlyCheckoutRoomAmount,
        BigDecimal earlyCheckoutEarlyCheckInFeeAmount,
        BigDecimal earlyCheckoutUnpaidBeforeCheckoutAmount,
        BigDecimal earlyCheckoutOutstandingAmount,
        BigDecimal earlyCheckoutBookingTotalAmount,
        BigDecimal earlyCheckoutPaidAmount,
        BigDecimal earlyCheckoutProjectedTotalAmount) {
    public record RoomStay(
            String bookingDetailId,
            String roomNumber,
            LocalDate checkinDate,
            LocalDate checkoutDate,
            String status,
            List<ServiceStay> services) {
    }

    public record ServiceStay(
            String name,
            int quantity,
            int usedQuantity,
            int refundableQuantity,
            BigDecimal refundableAmount) {
    }
}
