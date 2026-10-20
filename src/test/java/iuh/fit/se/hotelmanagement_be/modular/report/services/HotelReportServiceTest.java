package iuh.fit.se.hotelmanagement_be.modular.report.services;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingCancellationRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.OrderRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.PaymentTransactionRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HotelReportServiceTest {
    private BookingRepository bookingRepository;
    private BookingCancellationRepository bookingCancellationRepository;
    private OrderRepository orderRepository;
    private PaymentTransactionRepository paymentTransactionRepository;
    private RoomRepository roomRepository;
    private HotelReportService service;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        bookingCancellationRepository = mock(BookingCancellationRepository.class);
        orderRepository = mock(OrderRepository.class);
        paymentTransactionRepository = mock(PaymentTransactionRepository.class);
        roomRepository = mock(RoomRepository.class);
        service = new HotelReportService(
                bookingRepository,
                bookingCancellationRepository,
                orderRepository,
                paymentTransactionRepository,
                roomRepository
        );
    }

    @Test
    void returnsServiceRevenuePromotionsAndCurrentRoomStatusesForSelectedPeriod() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 2);
        when(bookingRepository.countCreatedBookingsForPeriod(
                7L,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        )).thenReturn(8L);
        PaymentTransactionRepository.CashFlowReportProjection cashFlow =
                mock(PaymentTransactionRepository.CashFlowReportProjection.class);
        when(cashFlow.getCashReceived()).thenReturn(new BigDecimal("500000"));
        when(cashFlow.getCashRefunded()).thenReturn(new BigDecimal("75000"));
        when(cashFlow.getCashChangeReturned()).thenReturn(new BigDecimal("25000"));
        when(paymentTransactionRepository.getCashFlowReport(
                7L,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                CashFlowType.RECEIPT,
                CashFlowType.REFUND,
                CashFlowType.CHANGE
        )).thenReturn(cashFlow);
        OrderRepository.DailyServiceRevenueProjection revenueRow =
                mock(OrderRepository.DailyServiceRevenueProjection.class);
        when(revenueRow.getRevenueDate()).thenReturn(startDate);
        when(revenueRow.getRevenue()).thenReturn(new BigDecimal("125000"));
        when(revenueRow.getClosedInvoiceCount()).thenReturn(2L);
        when(orderRepository.getDailyServiceRevenue(
                7L,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                OrderStatusType.CANCELLED,
                BookingStatus.CANCELLED
        )).thenReturn(List.of(revenueRow));

        BookingRepository.PromotionReportProjection promotionRow =
                mock(BookingRepository.PromotionReportProjection.class);
        when(promotionRow.getPromotionId()).thenReturn("PROMO-1");
        when(promotionRow.getPromotionName()).thenReturn("Ưu đãi tháng 10");
        when(promotionRow.getPromotionCode()).thenReturn("OCT10");
        when(promotionRow.getUsageCount()).thenReturn(3L);
        when(promotionRow.getSavings()).thenReturn(new BigDecimal("50000"));
        when(bookingRepository.getPromotionReport(
                7L,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                BookingStatus.CANCELLED
        )).thenReturn(List.of(promotionRow));

        BookingCancellationRepository.CancellationReportProjection onlineCancellation =
                mock(BookingCancellationRepository.CancellationReportProjection.class);
        when(onlineCancellation.getBookingId()).thenReturn("BK-ONLINE");
        when(onlineCancellation.getBookingChannel()).thenReturn(BookingChannel.ONLINE);
        when(onlineCancellation.getReason()).thenReturn("Đổi lịch");
        when(onlineCancellation.getCancelledAt()).thenReturn(startDate.atTime(10, 30));
        BookingCancellationRepository.CancellationReportProjection offlineCancellation =
                mock(BookingCancellationRepository.CancellationReportProjection.class);
        when(offlineCancellation.getBookingId()).thenReturn("BK-OFFLINE");
        when(offlineCancellation.getBookingChannel()).thenReturn(BookingChannel.OFFLINE);
        when(offlineCancellation.getCancelledAt()).thenReturn(startDate.atTime(11, 30));
        when(bookingCancellationRepository.getCancellationReport(
                7L,
                BookingStatus.CANCELLED,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(onlineCancellation, offlineCancellation));

        RoomRepository.RoomStatusReportProjection readyRooms =
                mock(RoomRepository.RoomStatusReportProjection.class);
        when(readyRooms.getRoomStatus()).thenReturn(RoomStatus.READY);
        when(readyRooms.getRoomCount()).thenReturn(4L);
        RoomRepository.RoomStatusReportProjection roomsWithoutStatus =
                mock(RoomRepository.RoomStatusReportProjection.class);
        when(roomsWithoutStatus.getRoomStatus()).thenReturn(null);
        when(roomsWithoutStatus.getRoomCount()).thenReturn(1L);
        when(roomRepository.getRoomStatusReport(7L)).thenReturn(List.of(readyRooms, roomsWithoutStatus));

        var report = service.getHotelReport(7L, startDate, endDate);

        assertEquals(new BigDecimal("125000"), report.serviceRevenue());
        assertEquals(8, report.totalBookingCount());
        assertEquals(new BigDecimal("500000"), report.cashReceived());
        assertEquals(new BigDecimal("75000"), report.cashRefunded());
        assertEquals(new BigDecimal("25000"), report.cashChangeReturned());
        assertEquals(new BigDecimal("400000"), report.netCashReceived());
        assertEquals(2, report.closedInvoiceCount());
        assertEquals(1, report.dailyServiceRevenue().size());
        assertEquals(3, report.promotionUsageCount());
        assertEquals(new BigDecimal("50000"), report.promotionSavings());
        assertEquals(2, report.cancelledBookingCount());
        assertEquals(1, report.onlineCancelledBookingCount());
        assertEquals(1, report.offlineCancelledBookingCount());
        assertEquals("Đổi lịch", report.cancelledBookings().get(0).reason());
        assertEquals("Không ghi nhận lý do", report.cancelledBookings().get(1).reason());
        assertEquals(5, report.totalRoomCount());
        assertEquals("UNKNOWN", report.roomStatuses().get(report.roomStatuses().size() - 1).status());
        assertEquals(1, report.roomStatuses().get(report.roomStatuses().size() - 1).roomCount());
    }
}
