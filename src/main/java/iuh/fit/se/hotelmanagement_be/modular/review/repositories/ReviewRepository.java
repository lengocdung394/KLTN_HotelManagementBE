package iuh.fit.se.hotelmanagement_be.modular.review.repositories;

import iuh.fit.se.hotelmanagement_be.modular.review.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByHotelIdOrderByCreatedAtDesc(Long hotelId);
    Optional<Review> findByBookingId(Long bookingId);
    boolean existsByBookingId(Long bookingId);
    List<Review> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
