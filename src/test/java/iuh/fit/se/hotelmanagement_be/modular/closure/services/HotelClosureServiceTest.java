package iuh.fit.se.hotelmanagement_be.modular.closure.services;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.closure.entities.HotelClosureSchedule;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void earlyCheckoutChargesOnlyNightsBeforeCheckoutDate() {
        BookingDetail detail = stay("2026-07-08", "2026-07-12");
        detail.setRoomSubTotal(400.0);

        assertEquals(200.00, service.prorateRoomAmount(detail, LocalDate.parse("2026-07-10")).doubleValue());
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
}
