package iuh.fit.se.hotelmanagement_be.modular.payment.repositories;

import iuh.fit.se.hotelmanagement_be.modular.payment.entities.CheckoutRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CheckoutRefundRepository extends JpaRepository<CheckoutRefund, String> {
    @Query("SELECT refund FROM CheckoutRefund refund WHERE refund.booking.hotel.id = :hotelId ORDER BY refund.requestedAt DESC")
    List<CheckoutRefund> findAllByHotelIdOrderByRequestedAtDesc(@Param("hotelId") Long hotelId);

    @Query("SELECT refund FROM CheckoutRefund refund WHERE refund.booking.hotel.id = :hotelId AND refund.id = :refundId")
    Optional<CheckoutRefund> findByIdAndHotelId(@Param("refundId") String refundId, @Param("hotelId") Long hotelId);

    List<CheckoutRefund> findAllByOrderByRequestedAtDesc();

    @Query("SELECT refund FROM CheckoutRefund refund WHERE refund.booking.id = :bookingId")
    Optional<CheckoutRefund> findByBookingId(@Param("bookingId") String bookingId);
}
