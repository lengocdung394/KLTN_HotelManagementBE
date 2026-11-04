package iuh.fit.se.hotelmanagement_be.modular.payment.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.auth.services.impl.EmailService;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingCancellationRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.impl.BookingSocketEmitter;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.PaymentRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.services.PaymentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class BookingCancellationServiceRefundTest {
    @Test
    void fixedPromotionRefundUsesWholeVndAndPreservesBreakdownTotal() {
        BookingCancellationService service = new BookingCancellationService(
                mock(BookingRepository.class),
                mock(BookingSocketEmitter.class),
                mock(BookingCancellationRepository.class),
                mock(PaymentService.class),
                mock(PaymentRepository.class),
                mock(EmailService.class),
                mock(RefundBankDetailsCrypto.class),
                mock(PayOSPayoutGateway.class));
        Order order = Order.builder()
                .roomTotalAmount(new BigDecimal("6000.00"))
                .serviceTotalAmount(new BigDecimal("250000.00"))
                .surchargeTotalAmount(BigDecimal.ZERO)
                .discountRoomAmount(BigDecimal.ZERO)
                .discountServiceAmount(BigDecimal.ZERO)
                .discountAmountTotal(new BigDecimal("10.00"))
                .paidAmount(new BigDecimal("255990.00"))
                .build();

        BookingCancellationService.RefundBreakdown result =
                service.calculateRefund(order, new BigDecimal("0.50"));

        assertEquals(new BigDecimal("252990.00"), result.total());
        assertEquals(new BigDecimal("3000.00"), result.roomRefund());
        assertEquals(new BigDecimal("249990.00"), result.serviceRefund());
        assertEquals(new BigDecimal("3000.00"), result.roomPenalty());
        assertEquals(result.roomRefund().add(result.serviceRefund()), result.total());
    }
}
