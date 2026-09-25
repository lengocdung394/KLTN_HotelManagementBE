package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
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
    private final BookingSocketEmitter bookingSocketEmitter;

    @Scheduled(fixedRate = 60000) // mỗi 1 phút
    public void releaseExpiredPendingBookings() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        List<Booking> expiredBookings = bookingRepository.findExpiredPendingBookings(threshold, BookingStatus.PENDING);

        if (expiredBookings.isEmpty()) {
            return;
        }

        for (Booking booking : expiredBookings) {
            if (booking.getBookingStatus().equals(BookingStatus.PENDING)) {
                booking.setBookingStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);

                if (booking.getHotel() != null) {
                    bookingSocketEmitter.emitRoomMatrixUpdate(booking.getHotel().getId());
                    log.info("Expired pending booking released: bookingId={}, hotelId={}",
                            booking.getId(), booking.getHotel().getId());
                }
            }

        }
    }
}