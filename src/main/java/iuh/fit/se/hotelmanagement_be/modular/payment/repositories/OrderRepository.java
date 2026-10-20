package iuh.fit.se.hotelmanagement_be.modular.payment.repositories;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    interface DailyServiceRevenueProjection {
        LocalDate getRevenueDate();
        BigDecimal getRevenue();
        Long getClosedInvoiceCount();
    }

    @Query("""
            SELECT FUNCTION('DATE', o.closeDate) AS revenueDate,
                   COALESCE(SUM(COALESCE(o.serviceTotalAmount, 0) - COALESCE(o.discountServiceAmount, 0)), 0) AS revenue,
                   COUNT(o) AS closedInvoiceCount
            FROM Order o
            WHERE o.booking.hotel.id = :hotelId
              AND o.closeDate >= :start
              AND o.closeDate < :end
              AND (o.orderStatus IS NULL OR o.orderStatus <> :cancelledStatus)
              AND (o.booking.bookingStatus IS NULL OR o.booking.bookingStatus <> :cancelledBookingStatus)
            GROUP BY FUNCTION('DATE', o.closeDate)
            ORDER BY FUNCTION('DATE', o.closeDate)
            """)
    List<DailyServiceRevenueProjection> getDailyServiceRevenue(
            @Param("hotelId") Long hotelId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("cancelledStatus") OrderStatusType cancelledStatus,
            @Param("cancelledBookingStatus") BookingStatus cancelledBookingStatus
    );

    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.paymentOrderCodes c WHERE c = :paymentOrderCode")
    boolean existsByPaymentOrderCode(@Param("paymentOrderCode") Long paymentOrderCode);

    @Query("SELECT o FROM Order o JOIN o.paymentOrderCodes c WHERE c = :paymentOrderCode")
    Optional<Order> findByPaymentOrderCodesContaining(@Param("paymentOrderCode") Long paymentOrderCode);

    @Query("SELECT o FROM Order o WHERE o.booking.id = :bookingId")
    Optional<Order> findByBookingId(@Param("bookingId") String bookingId);

    List<Order> findByOrderStatus(OrderStatusType orderStatus);

    @Query("SELECT o FROM Order o WHERE o.issueDate BETWEEN :startDate AND :endDate")
    List<Order> findByIssueDateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.booking.id = :bookingId")
    Optional<Order> findByBookingIdForUpdate(@Param("bookingId") String bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o JOIN o.paymentOrderCodes c WHERE c = :paymentOrderCode")
    Optional<Order> findByPaymentOrderCodeForUpdate(@Param("paymentOrderCode") Long paymentOrderCode);

}
