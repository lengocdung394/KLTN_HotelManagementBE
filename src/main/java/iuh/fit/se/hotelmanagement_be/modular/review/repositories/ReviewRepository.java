package iuh.fit.se.hotelmanagement_be.modular.review.repositories;

import iuh.fit.se.hotelmanagement_be.modular.review.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByHotelIdOrderByCreatedAtDesc(Long hotelId);
    Optional<Review> findByBookingId(String bookingId);
    boolean existsByBookingId(String bookingId);
    List<Review> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
