package iuh.fit.se.hotelmanagement_be.modular.booking.repositories;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking,Long> {
}
