package iuh.fit.se.hotelmanagement_be.modular.booking.repositories;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingServiceUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingServiceUsageRepository extends JpaRepository<BookingServiceUsage, String> {
    boolean existsByBookingServiceDetail_Id(String bookingServiceDetailId);
}
