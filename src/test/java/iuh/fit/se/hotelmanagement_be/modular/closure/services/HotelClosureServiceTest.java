package iuh.fit.se.hotelmanagement_be.modular.closure.services;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingNightlyPrice;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingServiceDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.CheckInOutService;
import iuh.fit.se.hotelmanagement_be.modular.closure.entities.HotelClosureSchedule;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HotelClosureServiceTest {
    private final HotelClosureService service = new HotelClosureService(
            null, null, null, null, null, null, null);

    @Test
    void checkoutOnFirstClosureDateDoesNotOverlapClosedNights() {
        HotelClosureSchedule schedule = schedule(LocalDate.parse("2026-07-10"), LocalDate.parse("2026-07-12"));
        BookingDetail detail = stay("2026-07-08", "2026-07-10");

        assertFalse(service.overlaps(schedule, detail));
    }

    @Test
    void stayIncludingLastClosureDateOverlaps() {
        HotelClosureSchedule schedule = schedule(LocalDate.parse("2026-07-10"), LocalDate.parse("2026-07-12"));
        BookingDetail detail = stay("2026-07-12", "2026-07-14");

        assertTrue(service.overlaps(schedule, detail));
    }

    @Test
    void closureScheduleConflictsWhenDateAndHotelOverlapInclusively() {
        HotelClosureSchedule existing = schedule(LocalDate.parse("2026-07-10"), LocalDate.parse("2026-07-12"));
        existing.setHotelIds(List.of(1L, 2L));

        assertTrue(service.scheduleConflictsWith(
                existing, LocalDate.parse("2026-07-12"), LocalDate.parse("2026-07-13"),
                false, List.of(2L)));
        assertFalse(service.scheduleConflictsWith(
                existing, LocalDate.parse("2026-07-13"), LocalDate.parse("2026-07-14"),
                false, List.of(2L)));
        assertFalse(service.scheduleConflictsWith(
                existing, LocalDate.parse("2026-07-11"), LocalDate.parse("2026-07-12"),
                false, List.of(3L)));
    }

    @Test
    void allHotelClosureScheduleConflictsWithAnyBranchForOverlappingDates() {
        HotelClosureSchedule existing = schedule(LocalDate.parse("2026-07-10"), LocalDate.parse("2026-07-12"));
        existing.setAllHotels(true);
        existing.setHotelIds(List.of());

        assertTrue(service.scheduleConflictsWith(
                existing, LocalDate.parse("2026-07-11"), LocalDate.parse("2026-07-13"),
                false, List.of(3L)));
    }

    @Test
    void earlyCheckoutChargesOnlyNightsBeforeCheckoutDate() {
        BookingDetail detail = stay("2026-07-08", "2026-07-12");
        detail.setRoomSubTotal(400.0);

        assertEquals(200.00, service.prorateRoomAmount(detail, LocalDate.parse("2026-07-10")).doubleValue());
    }

    @Test
    void earlyCheckoutOnCheckInDateChargesTheFirstNight() {
        LocalDate checkinDate = LocalDate.now();
        BookingDetail detail = stay(checkinDate.toString(), checkinDate.plusDays(3).toString());
        detail.setRoomSubTotal(6000.0);

        assertEquals(2000.00, service.prorateRoomAmount(detail, checkinDate).doubleValue());
    }

    @Test
    void earlyCheckoutProratesRoomAndOrderWideDiscounts() {
        Order order = new Order();
        order.setDiscountRoomAmount(new java.math.BigDecimal("20.00"));
        order.setDiscountAmountTotal(new java.math.BigDecimal("40.00"));
        order.setServiceTotalAmount(java.math.BigDecimal.ZERO);
        order.setSurchargeTotalAmount(java.math.BigDecimal.ZERO);

        service.prorateOrderDiscounts(
                order,
                new java.math.BigDecimal("400.00"),
                new java.math.BigDecimal("200.00"));

        assertEquals(new java.math.BigDecimal("10.00"), order.getDiscountRoomAmount());
        assertEquals(new java.math.BigDecimal("20.00"), order.getDiscountAmountTotal());
    }

    @Test
    void previewsEarlyCheckoutRefundAfterProratingRoomDiscount() {
        CheckInOutService checkInOutService = mock(CheckInOutService.class);
        when(checkInOutService.previewLateCheckOutFee(any(), any())).thenReturn(BigDecimal.ZERO);
        HotelClosureService previewService = new HotelClosureService(
                null, null, null, null, null, null, checkInOutService);
        LocalDate today = LocalDate.now();

        Booking booking = new Booking();
        Order order = new Order();
        order.setPaidAmount(new BigDecimal("400.00"));
        order.setRoomTotalAmount(new BigDecimal("400.00"));
        order.setServiceTotalAmount(BigDecimal.ZERO);
        order.setSurchargeTotalAmount(BigDecimal.ZERO);
        order.setDiscountRoomAmount(new BigDecimal("40.00"));
        order.setDiscountServiceAmount(BigDecimal.ZERO);
        order.setDiscountAmountTotal(BigDecimal.ZERO);
        booking.setOrder(order);

        BookingDetail detail = stay(today.minusDays(1).toString(), today.plusDays(3).toString());
        detail.setStatus(BookingStatusType.CHECKED_IN);
        detail.setRoomSubTotal(400.0);
        detail.setBooking(booking);
        detail.setNightlyPrices(List.of(
                nightlyPrice(today.minusDays(1), "100.00"),
                nightlyPrice(today, "100.00"),
                nightlyPrice(today.plusDays(1), "100.00"),
                nightlyPrice(today.plusDays(2), "100.00")));
        booking.addBookingDetail(detail);

        assertEquals(new BigDecimal("310.00"),
                previewService.previewEarlyCheckoutRefund(booking, List.of(detail)));
    }

    @Test
    void previewsRefundForUnusedServiceQuantityOnEarlyCheckout() {
        CheckInOutService checkInOutService = mock(CheckInOutService.class);
        when(checkInOutService.previewLateCheckOutFee(any(), any())).thenReturn(BigDecimal.ZERO);
        HotelClosureService previewService = new HotelClosureService(
                null, null, null, null, null, null, checkInOutService);
        LocalDate today = LocalDate.now();

        Booking booking = new Booking();
        Order order = new Order();
        order.setPaidAmount(new BigDecimal("500.00"));
        order.setRoomTotalAmount(new BigDecimal("400.00"));
        order.setServiceTotalAmount(new BigDecimal("100.00"));
        order.setSurchargeTotalAmount(BigDecimal.ZERO);
        order.setDiscountRoomAmount(BigDecimal.ZERO);
        order.setDiscountServiceAmount(BigDecimal.ZERO);
        order.setDiscountAmountTotal(BigDecimal.ZERO);
        booking.setOrder(order);

        BookingDetail detail = stay(today.minusDays(1).toString(), today.plusDays(3).toString());
        detail.setStatus(BookingStatusType.CHECKED_IN);
        detail.setRoomSubTotal(400.0);
        detail.setBooking(booking);
        detail.setNightlyPrices(List.of(
                nightlyPrice(today.minusDays(1), "100.00"),
                nightlyPrice(today, "100.00"),
                nightlyPrice(today.plusDays(1), "100.00"),
                nightlyPrice(today.plusDays(2), "100.00")));
        BookingServiceDetail service = new BookingServiceDetail();
        service.setName("Ăn sáng");
        service.setPrice(50.0);
        service.setQuantity(2);
        service.setUsedQuantity(1);
        service.setUsed(false);
        detail.setBookingServiceDetails(List.of(service));
        booking.addBookingDetail(detail);

        assertEquals(new BigDecimal("350.00"),
                previewService.previewEarlyCheckoutRefund(booking, List.of(detail)));
    }

    @Test
    void sameDayEarlyCheckoutRefundDeductsFirstRoomNightAndKeepsUnusedServiceRefund() {
        CheckInOutService checkInOutService = mock(CheckInOutService.class);
        when(checkInOutService.previewLateCheckOutFee(any(), any())).thenReturn(BigDecimal.ZERO);
        HotelClosureService previewService = new HotelClosureService(
                null, null, null, null, null, null, checkInOutService);
        LocalDate today = LocalDate.now();

        Booking booking = new Booking();
        Order order = new Order();
        order.setPaidAmount(new BigDecimal("206000.00"));
        order.setTotalAmount(new BigDecimal("206600.00"));
        order.setRoomTotalAmount(new BigDecimal("6000.00"));
        order.setServiceTotalAmount(new BigDecimal("200000.00"));
        order.setSurchargeTotalAmount(new BigDecimal("600.00"));
        order.setDiscountRoomAmount(BigDecimal.ZERO);
        order.setDiscountServiceAmount(BigDecimal.ZERO);
        order.setDiscountAmountTotal(BigDecimal.ZERO);
        booking.setOrder(order);

        BookingDetail detail = stay(today.toString(), today.plusDays(3).toString());
        detail.setStatus(BookingStatusType.CHECKED_IN);
        detail.setRoomSubTotal(6000.0);
        detail.setEarlyCheckInFee(new BigDecimal("600.00"));
        detail.setBooking(booking);
        detail.setNightlyPrices(List.of(
                nightlyPrice(today, "2000.00"),
                nightlyPrice(today.plusDays(1), "2000.00"),
                nightlyPrice(today.plusDays(2), "2000.00")));
        BookingServiceDetail service = new BookingServiceDetail();
        service.setName("Massage");
        service.setPrice(200000.0);
        service.setQuantity(1);
        service.setUsedQuantity(0);
        service.setUsed(false);
        detail.setBookingServiceDetails(List.of(service));
        booking.addBookingDetail(detail);

        var preview = previewService.previewEarlyCheckout(booking, List.of(detail));
        assertEquals(new BigDecimal("203400.00"), preview.refundAmount());
        assertEquals(new BigDecimal("6000.00"), preview.originalRoomAmount());
        assertEquals(3, preview.originalRoomNights());
        assertEquals(1, preview.usedRoomNights());
        assertEquals(new BigDecimal("2000.00"), preview.roomAmount());
        assertEquals(new BigDecimal("600.00"), preview.earlyCheckInFeeAmount());
        assertEquals(new BigDecimal("206600.00"), preview.bookingTotalAmount());
        assertEquals(new BigDecimal("206000.00"), preview.paidAmount());
        assertEquals(new BigDecimal("600.00"), preview.unpaidBeforeCheckoutAmount());
        assertEquals(new BigDecimal("0.00"), preview.outstandingAmount());
        assertEquals(new BigDecimal("2600.00"), preview.projectedTotalAmount());
    }

    private static HotelClosureSchedule schedule(LocalDate start, LocalDate end) {
        HotelClosureSchedule schedule = new HotelClosureSchedule();
        schedule.setStartDate(start);
        schedule.setEndDate(end);
        return schedule;
    }

    private static BookingDetail stay(String checkin, String checkout) {
        BookingDetail detail = new BookingDetail();
        detail.setCheckinTime(LocalDate.parse(checkin).atStartOfDay());
        detail.setCheckoutTime(LocalDate.parse(checkout).atStartOfDay());
        return detail;
    }

    private static BookingNightlyPrice nightlyPrice(LocalDate date, String amount) {
        BookingNightlyPrice price = new BookingNightlyPrice();
        price.setStayDate(date);
        price.setNightTotal(new BigDecimal(amount));
        return price;
    }
}
