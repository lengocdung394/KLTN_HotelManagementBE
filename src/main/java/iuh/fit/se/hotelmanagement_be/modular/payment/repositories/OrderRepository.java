package iuh.fit.se.hotelmanagement_be.modular.payment.repositories;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
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
        Double getRevenue();
        Long getServiceUsageCount();
    }

    @Query("""
            SELECT FUNCTION('DATE', COALESCE(service.usedAt, detail.actualCheckOutTime, detail.checkoutTime)) AS revenueDate,
                   COALESCE(SUM(COALESCE(service.price, 0) * service.quantity), 0) AS revenue,
                   COUNT(service.id) AS serviceUsageCount
            FROM BookingServiceDetail service
            JOIN service.bookingDetail detail
            JOIN detail.booking booking
            WHERE booking.hotel.id = :hotelId
              AND COALESCE(service.usedAt, detail.actualCheckOutTime, detail.checkoutTime) >= :start
              AND COALESCE(service.usedAt, detail.actualCheckOutTime, detail.checkoutTime) < :end
              AND service.used = true
              AND (service.cancelled = false OR service.cancelled IS NULL)
              AND (detail.status IS NULL OR detail.status <> :cancelledRoomStatus)
              AND (booking.bookingStatus IS NULL OR booking.bookingStatus <> :cancelledBookingStatus)
            GROUP BY FUNCTION('DATE', COALESCE(service.usedAt, detail.actualCheckOutTime, detail.checkoutTime))
            ORDER BY FUNCTION('DATE', COALESCE(service.usedAt, detail.actualCheckOutTime, detail.checkoutTime))
            """)
    List<DailyServiceRevenueProjection> getDailyServiceRevenue(
            @Param("hotelId") Long hotelId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("cancelledRoomStatus") BookingStatusType cancelledRoomStatus,
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
