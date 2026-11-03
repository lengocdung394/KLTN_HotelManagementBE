package iuh.fit.se.hotelmanagement_be.modular.payment.repositories;

import iuh.fit.se.hotelmanagement_be.modular.payment.entities.CheckoutRefund;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.CheckoutRefundStatus;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CheckoutRefundRepository extends JpaRepository<CheckoutRefund, String> {
    interface RefundReportProjection {
        String getBookingId();
        BigDecimal getAmount();
        PaymentType getPaymentMethod();
        CheckoutRefundStatus getStatus();
        LocalDateTime getRequestedAt();
        LocalDateTime getRefundedAt();
    }

    @Query("""
            SELECT booking.id AS bookingId,
                   refund.amount AS amount,
                   refund.refundMethod AS paymentMethod,
                   refund.status AS status,
                   refund.requestedAt AS requestedAt,
                   refund.refundedAt AS refundedAt
            FROM CheckoutRefund refund
            JOIN refund.booking booking
            WHERE booking.hotel.id = :hotelId
              AND COALESCE(refund.refundedAt, refund.requestedAt) >= :start
              AND COALESCE(refund.refundedAt, refund.requestedAt) < :end
            ORDER BY COALESCE(refund.refundedAt, refund.requestedAt) DESC
            """)
    List<RefundReportProjection> getRefundReport(
            @Param("hotelId") Long hotelId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT refund FROM CheckoutRefund refund WHERE refund.booking.hotel.id = :hotelId ORDER BY refund.requestedAt DESC")
    List<CheckoutRefund> findAllByHotelIdOrderByRequestedAtDesc(@Param("hotelId") Long hotelId);

    @Query("SELECT refund FROM CheckoutRefund refund WHERE refund.booking.hotel.id = :hotelId AND refund.id = :refundId")
    Optional<CheckoutRefund> findByIdAndHotelId(@Param("refundId") String refundId, @Param("hotelId") Long hotelId);

    List<CheckoutRefund> findAllByOrderByRequestedAtDesc();

    @Query("SELECT refund FROM CheckoutRefund refund WHERE refund.booking.id = :bookingId")
    Optional<CheckoutRefund> findByBookingId(@Param("bookingId") String bookingId);
}
