package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.services.impl.BookingCancellationService;
import iuh.fit.se.hotelmanagement_be.modular.payment.services.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;
    private final BookingCancellationService bookingCancellationService;
    private final PaymentService paymentService;

    @Scheduled(fixedRate = 5000)
    public void releaseExpiredPendingBookings() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        List<Booking> expiredBookings = bookingRepository.findExpiredPendingBookings(threshold, BookingStatus.PENDING);

        if (expiredBookings.isEmpty()) {
            return;
        }

        for (Booking booking : expiredBookings) {
            if (booking.getBookingStatus() != BookingStatus.PENDING) {
                continue;
            }
            try {
                if (booking.getOrder() != null) {
                    Map<String, Object> paymentStatus =
                            paymentService.checkOrderStatus(booking.getOrder().getId());
                    if (Boolean.TRUE.equals(paymentStatus.get("isPaid"))) {
                        continue;
                    }
                    if ("UNAVAILABLE".equals(paymentStatus.get("gatewayStatus"))) {
                        log.warn("Deferring expiry because PayOS status could not be verified: bookingId={}, orderId={}",
                                booking.getId(), booking.getOrder().getId());
                        continue;
                    }
                }
                bookingCancellationService.expirePendingBooking(booking.getId());
            } catch (RuntimeException exception) {
                log.error("Could not verify or expire pending booking; will retry: bookingId={}, hotelId={}",
                        booking.getId(), booking.getHotel() == null ? null : booking.getHotel().getId(), exception);
            }
        }
    }
}
