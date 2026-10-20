package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.services.impl.BookingCancellationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;
    private final BookingCancellationService bookingCancellationService;

    @Scheduled(fixedRate = 60000) // mỗi 1 phút
    public void releaseExpiredPendingBookings() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        List<Booking> expiredBookings = bookingRepository.findExpiredPendingBookings(threshold, BookingStatus.PENDING);

        if (expiredBookings.isEmpty()) {
            return;
        }

        for (Booking booking : expiredBookings) {
            if (booking.getBookingStatus().equals(BookingStatus.PENDING)) {
                bookingCancellationService.expirePendingBooking(booking.getId());
                log.info("Expired pending booking processed: bookingId={}, hotelId={}",
                        booking.getId(), booking.getHotel() == null ? null : booking.getHotel().getId());
            }
        }
    }
}