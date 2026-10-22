package iuh.fit.se.hotelmanagement_be.modular.report.services;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingCancellationRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.OrderRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.PaymentTransactionRepository;
import iuh.fit.se.hotelmanagement_be.modular.report.responses.DailyServiceRevenueResponse;
import iuh.fit.se.hotelmanagement_be.modular.report.responses.BookingCancellationReportResponse;
import iuh.fit.se.hotelmanagement_be.modular.report.responses.HotelReportResponse;
import iuh.fit.se.hotelmanagement_be.modular.report.responses.PromotionUsageReportResponse;
import iuh.fit.se.hotelmanagement_be.modular.report.responses.RoomStatusReportResponse;
import iuh.fit.se.hotelmanagement_be.modular.report.responses.RoomTypeBookingReportResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotelReportService {
    private final BookingRepository bookingRepository;
    private final BookingCancellationRepository bookingCancellationRepository;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RoomRepository roomRepository;

    public HotelReportResponse getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        long totalBookingCount = bookingRepository.countCreatedBookingsForPeriod(hotelId, start, endExclusive);
        Map<RoomType, Long> roomTypeBookingCounts = new EnumMap<>(RoomType.class);
        for (RoomType roomType : RoomType.values()) {
            roomTypeBookingCounts.put(roomType, 0L);
        }
        for (BookingRepository.RoomTypeBookingReportProjection row :
                bookingRepository.getRoomTypeBookingReport(
                        hotelId,
                        start,
                        endExclusive,
                        BookingStatus.CANCELLED,
                        BookingStatusType.CANCELLED
                )) {
            if (row.getRoomType() != null) {
                roomTypeBookingCounts.put(
                        row.getRoomType(),
                        row.getBookingCount() == null ? 0 : row.getBookingCount()
                );
            }
        }
        List<RoomTypeBookingReportResponse> roomTypeBookings = roomTypeBookingCounts.entrySet().stream()
                .map(entry -> new RoomTypeBookingReportResponse(entry.getKey().name(), entry.getValue()))
                .toList();
        PaymentTransactionRepository.CashFlowReportProjection cashFlow =
                paymentTransactionRepository.getCashFlowReport(
                        hotelId,
                        start,
                        endExclusive,
                        CashFlowType.RECEIPT,
                        CashFlowType.REFUND,
                        CashFlowType.CHANGE
                );
        BigDecimal cashReceived = cashFlow.getCashReceived() == null
                ? BigDecimal.ZERO
                : cashFlow.getCashReceived();
        BigDecimal cashRefunded = cashFlow.getCashRefunded() == null
                ? BigDecimal.ZERO
                : cashFlow.getCashRefunded();
        BigDecimal cashChangeReturned = cashFlow.getCashChangeReturned() == null
                ? BigDecimal.ZERO
                : cashFlow.getCashChangeReturned();

        List<OrderRepository.DailyServiceRevenueProjection> revenueRows =
                orderRepository.getDailyServiceRevenue(
                        hotelId,
                        start,
                        endExclusive,
                        BookingStatusType.CANCELLED,
                        BookingStatus.CANCELLED
                );
        BigDecimal serviceRevenue = BigDecimal.ZERO;
        long serviceUsageCount = 0;
        List<DailyServiceRevenueResponse> dailyServiceRevenue = new ArrayList<>();
        for (OrderRepository.DailyServiceRevenueProjection row : revenueRows) {
            BigDecimal revenue = row.getRevenue() == null
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(row.getRevenue());
            long usageCount = row.getServiceUsageCount() == null ? 0 : row.getServiceUsageCount();
            serviceRevenue = serviceRevenue.add(revenue);
            serviceUsageCount += usageCount;
            dailyServiceRevenue.add(new DailyServiceRevenueResponse(row.getRevenueDate(), revenue, usageCount));
        }

        List<BookingRepository.PromotionReportProjection> promotionRows =
                bookingRepository.getPromotionReport(hotelId, start, endExclusive, BookingStatus.CANCELLED);
        List<PromotionUsageReportResponse> promotions = promotionRows.stream()
                .map(row -> new PromotionUsageReportResponse(
                        row.getPromotionId(),
                        row.getPromotionName(),
                        row.getPromotionCode(),
                        row.getUsageCount() == null ? 0 : row.getUsageCount(),
                        row.getSavings() == null ? BigDecimal.ZERO : row.getSavings()
                ))
                .toList();
        long promotionUsageCount = promotions.stream().mapToLong(PromotionUsageReportResponse::usageCount).sum();
        BigDecimal promotionSavings = promotions.stream()
                .map(PromotionUsageReportResponse::savings)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<BookingCancellationRepository.CancellationReportProjection> cancellationRows =
                bookingCancellationRepository.getCancellationReport(
                        hotelId,
                        BookingStatus.CANCELLED,
                        start,
                        endExclusive
                );
        long onlineCancelledBookingCount = cancellationRows.stream()
                .filter(row -> row.getBookingChannel() == BookingChannel.ONLINE)
                .count();
        long offlineCancelledBookingCount = cancellationRows.stream()
                .filter(row -> row.getBookingChannel() == BookingChannel.OFFLINE)
                .count();
        List<BookingCancellationReportResponse> cancelledBookings = cancellationRows.stream()
                .map(row -> new BookingCancellationReportResponse(
                        row.getBookingId(),
                        row.getBookingChannel() == null ? "UNKNOWN" : row.getBookingChannel().name(),
                        row.getReason() == null || row.getReason().isBlank()
                                ? "Không ghi nhận lý do"
                                : row.getReason(),
                        row.getCancelledAt()
                ))
                .toList();

        Map<RoomStatus, Long> roomCounts = new EnumMap<>(RoomStatus.class);
        for (RoomStatus status : RoomStatus.values()) {
            roomCounts.put(status, 0L);
        }
        long roomsWithoutStatus = 0;
        for (RoomRepository.RoomStatusReportProjection row : roomRepository.getRoomStatusReport(hotelId)) {
            long count = row.getRoomCount() == null ? 0 : row.getRoomCount();
            if (row.getRoomStatus() == null) {
                roomsWithoutStatus += count;
            } else {
                roomCounts.put(row.getRoomStatus(), count);
            }
        }
        List<RoomStatusReportResponse> roomStatuses = new ArrayList<>();
        roomCounts.forEach((status, count) ->
                roomStatuses.add(new RoomStatusReportResponse(status.name(), count)));
        if (roomsWithoutStatus > 0) {
            roomStatuses.add(new RoomStatusReportResponse("UNKNOWN", roomsWithoutStatus));
        }
        long totalRoomCount = roomStatuses.stream().mapToLong(RoomStatusReportResponse::roomCount).sum();

        return new HotelReportResponse(
                startDate,
                endDate,
                totalBookingCount,
                cashReceived,
                cashRefunded,
                cashChangeReturned,
                cashReceived.subtract(cashRefunded).subtract(cashChangeReturned),
                serviceRevenue,
                serviceUsageCount,
                dailyServiceRevenue,
                promotionUsageCount,
                promotionSavings,
                promotions,
                cancelledBookings.size(),
                onlineCancelledBookingCount,
                offlineCancelledBookingCount,
                cancelledBookings,
                totalRoomCount,
                roomStatuses,
                roomTypeBookings
        );
    }
}
