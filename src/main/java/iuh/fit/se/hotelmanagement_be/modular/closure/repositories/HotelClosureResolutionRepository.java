package iuh.fit.se.hotelmanagement_be.modular.closure.repositories;

import iuh.fit.se.hotelmanagement_be.modular.closure.entities.HotelClosureResolution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HotelClosureResolutionRepository extends JpaRepository<HotelClosureResolution, String> {
    Optional<HotelClosureResolution> findBySchedule_IdAndBooking_Id(String scheduleId, String bookingId);
}
