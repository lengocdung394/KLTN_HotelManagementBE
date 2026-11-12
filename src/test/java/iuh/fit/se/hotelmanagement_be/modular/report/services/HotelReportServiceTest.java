package iuh.fit.se.hotelmanagement_be.modular.report.services;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingCancellationStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingNightlyPrice;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingCancellationRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentType;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.CheckoutRefundRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.OrderRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.PaymentTransactionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.UserPromotionUsageRepository;
import iuh.fit.se.hotelmanagement_be.modular.report.responses.RoomTypeBookingReportResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
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
    private CheckoutRefundRepository checkoutRefundRepository;
    private RoomRepository roomRepository;
    private UserPromotionUsageRepository userPromotionUsageRepository;
    private HotelReportService service;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        bookingCancellationRepository = mock(BookingCancellationRepository.class);
        orderRepository = mock(OrderRepository.class);
        paymentTransactionRepository = mock(PaymentTransactionRepository.class);
        checkoutRefundRepository = mock(CheckoutRefundRepository.class);
        roomRepository = mock(RoomRepository.class);
        userPromotionUsageRepository = mock(UserPromotionUsageRepository.class);
        service = new HotelReportService(
                bookingRepository,
                bookingCancellationRepository,
                orderRepository,
                paymentTransactionRepository,
                checkoutRefundRepository,
                roomRepository,
                userPromotionUsageRepository
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
        BookingDetail roomRevenueDetail = new BookingDetail();
        roomRevenueDetail.setCheckinTime(startDate.atTime(14, 0));
        roomRevenueDetail.setCheckoutTime(endDate.plusDays(1).atTime(12, 0));
        roomRevenueDetail.setStatus(BookingStatusType.CHECKED_OUT);
        roomRevenueDetail.setNightlyPrices(List.of(
                nightlyPrice(startDate, "120000"),
                nightlyPrice(endDate, "130000"),
                nightlyPrice(endDate.plusDays(1), "140000")
        ));
        BookingDetail legacyRoomRevenueDetail = new BookingDetail();
        legacyRoomRevenueDetail.setCheckinTime(startDate.minusDays(1).atTime(14, 0));
        legacyRoomRevenueDetail.setCheckoutTime(endDate.atTime(12, 0));
        legacyRoomRevenueDetail.setStatus(BookingStatusType.CHECKED_OUT);
        legacyRoomRevenueDetail.setRoomSubTotal(200000.0);
        when(bookingRepository.findRoomRevenueDetailsForPeriod(
                7L,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                BookingStatus.cancelledStatuses(),
                BookingStatusType.CHECKED_OUT
        )).thenReturn(List.of(roomRevenueDetail, legacyRoomRevenueDetail));
        BookingRepository.RoomTypeBookingReportProjection standardRoomBookings =
                mock(BookingRepository.RoomTypeBookingReportProjection.class);
        when(standardRoomBookings.getRoomType()).thenReturn(RoomType.STANDARD);
        when(standardRoomBookings.getBookingCount()).thenReturn(3L);
        BookingRepository.RoomTypeBookingReportProjection familyRoomBookings =
                mock(BookingRepository.RoomTypeBookingReportProjection.class);
        when(familyRoomBookings.getRoomType()).thenReturn(RoomType.FAMILY);
        when(familyRoomBookings.getBookingCount()).thenReturn(1L);
        when(bookingRepository.getRoomTypeBookingReport(
                7L,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                BookingStatus.cancelledStatuses(),
                BookingStatusType.CANCELLED
        )).thenReturn(List.of(standardRoomBookings, familyRoomBookings));
        PaymentTransactionRepository.CashFlowReportProjection cashFlow =
                mock(PaymentTransactionRepository.CashFlowReportProjection.class);
        when(cashFlow.getCashReceived()).thenReturn(new BigDecimal("500000"));
        when(cashFlow.getCashReceivedCash()).thenReturn(new BigDecimal("200000"));
        when(cashFlow.getCashReceivedBank()).thenReturn(new BigDecimal("300000"));
        when(cashFlow.getCashRefunded()).thenReturn(new BigDecimal("75000"));
        when(cashFlow.getCashChangeReturned()).thenReturn(new BigDecimal("25000"));
        when(paymentTransactionRepository.getCashFlowReport(
                7L,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                CashFlowType.RECEIPT,
                CashFlowType.REFUND,
                CashFlowType.CHANGE,
                PaymentType.CASH,
                PaymentType.BANK
        )).thenReturn(cashFlow);
        OrderRepository.DailyServiceRevenueProjection revenueRow =
                mock(OrderRepository.DailyServiceRevenueProjection.class);
        when(revenueRow.getRevenueDate()).thenReturn(startDate);
        when(revenueRow.getRevenue()).thenReturn(125000.0);
        when(revenueRow.getServiceUsageCount()).thenReturn(2L);
        when(orderRepository.getDailyServiceRevenue(
                7L,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                BookingStatusType.CHECKED_OUT,
                BookingStatus.cancelledStatuses()
        )).thenReturn(List.of(revenueRow));

        UserPromotionUsageRepository.PromotionUsageReportProjection promotionRow =
                mock(UserPromotionUsageRepository.PromotionUsageReportProjection.class);
        when(promotionRow.getPromotionId()).thenReturn("PROMO-1");
        when(promotionRow.getPromotionName()).thenReturn("Ưu đãi tháng 10");
        when(promotionRow.getPromotionCode()).thenReturn("OCT10");
        when(promotionRow.getUsageCount()).thenReturn(3L);
        when(promotionRow.getUserCount()).thenReturn(2L);
        when(promotionRow.getSavings()).thenReturn(new BigDecimal("50000"));
        when(userPromotionUsageRepository.getPromotionReport(
                7L,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                BookingStatus.cancelledStatuses()
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
                BookingStatus.cancelledStatuses(),
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(onlineCancellation, offlineCancellation));
        when(checkoutRefundRepository.getRefundReport(
                7L,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());
        when(bookingCancellationRepository.getRefundReport(
                7L,
                BookingCancellationStatus.NO_REFUND_REQUIRED,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());

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

        assertEquals(0, new BigDecimal("125000").compareTo(report.serviceRevenue()));
        assertEquals(8, report.totalBookingCount());
        assertEquals(List.of(
                new RoomTypeBookingReportResponse("STANDARD", 3),
                new RoomTypeBookingReportResponse("DELUXE", 0),
                new RoomTypeBookingReportResponse("SUITE", 0),
                new RoomTypeBookingReportResponse("FAMILY", 1)
        ), report.roomTypeBookings());
        assertEquals(new BigDecimal("500000"), report.cashReceived());
        assertEquals(new BigDecimal("200000"), report.cashReceivedCash());
        assertEquals(new BigDecimal("300000"), report.cashReceivedBank());
        assertEquals(new BigDecimal("75000"), report.cashRefunded());
        assertEquals(new BigDecimal("25000"), report.cashChangeReturned());
        assertEquals(new BigDecimal("400000"), report.netCashReceived());
        assertEquals(0, new BigDecimal("350000").compareTo(report.roomRevenue()));
        assertEquals(2, report.serviceUsageCount());
        assertEquals(1, report.dailyServiceRevenue().size());
        assertEquals(3, report.promotionUsageCount());
        assertEquals(new BigDecimal("50000"), report.promotionSavings());
        assertEquals(3, report.promotions().get(0).usageCount());
        assertEquals(2, report.promotions().get(0).userCount());
        assertEquals(2, report.cancelledBookingCount());
        assertEquals(1, report.onlineCancelledBookingCount());
        assertEquals(1, report.offlineCancelledBookingCount());
        assertEquals("Đổi lịch", report.cancelledBookings().get(0).reason());
        assertEquals("Không ghi nhận lý do", report.cancelledBookings().get(1).reason());
        assertEquals(0, report.checkoutSurplusRefunds().size());
        assertEquals(0, report.bookingCancellationRefunds().size());
        assertEquals(5, report.totalRoomCount());
        assertEquals("UNKNOWN", report.roomStatuses().get(report.roomStatuses().size() - 1).status());
        assertEquals(1, report.roomStatuses().get(report.roomStatuses().size() - 1).roomCount());
    }

    private BookingNightlyPrice nightlyPrice(LocalDate stayDate, String amount) {
        BookingNightlyPrice price = new BookingNightlyPrice();
        price.setStayDate(stayDate);
        price.setNightTotal(new BigDecimal(amount));
        return price;
    }
}
