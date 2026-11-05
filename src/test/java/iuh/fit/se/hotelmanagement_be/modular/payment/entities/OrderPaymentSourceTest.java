package iuh.fit.se.hotelmanagement_be.modular.payment.entities;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentSource;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderPaymentSourceTest {

    @Test
    void recordsCounterEmployeeForPayOsPaymentOnOfflineBooking() {
        Employee employee = Employee.builder()
                .id("EMP-1")
                .fullName("Front desk employee")
                .build();
        Booking booking = Booking.builder()
                .bookingChannel(BookingChannel.OFFLINE)
                .employee(employee)
                .build();
        Order order = Order.builder().booking(booking).build();

        order.addPaymentSuccess(BigDecimal.TEN, PaymentType.BANK, "link-1");

        PaymentTransaction transaction = order.getPaymentTransactions().get(0);
        assertEquals(PaymentSource.FRONT_DESK, transaction.getPaymentSource());
        assertEquals(employee, transaction.getProcessorEmployee());
    }

    @Test
    void recordsOnlineBookingWithoutProcessorEmployee() {
        Booking booking = Booking.builder()
                .bookingChannel(BookingChannel.ONLINE)
                .build();
        Order order = Order.builder().booking(booking).build();

        order.addPaymentSuccess(BigDecimal.TEN, PaymentType.BANK, "link-2");

        PaymentTransaction transaction = order.getPaymentTransactions().get(0);
        assertEquals(PaymentSource.ONLINE_BOOKING, transaction.getPaymentSource());
        assertNull(transaction.getProcessorEmployee());
    }
}
