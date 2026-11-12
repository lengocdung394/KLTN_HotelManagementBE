package iuh.fit.se.hotelmanagement_be.modular.payment.repositories;

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
            SELECT FUNCTION('DATE', serviceUsage.confirmedAt) AS revenueDate,
                   COALESCE(SUM(COALESCE(serviceUsage.unitPrice, 0) * serviceUsage.quantity), 0) AS revenue,
                   COUNT(serviceUsage.id) AS serviceUsageCount
            FROM BookingServiceUsage serviceUsage
            JOIN serviceUsage.bookingServiceDetail service
            JOIN service.bookingDetail detail
            JOIN detail.booking booking
            WHERE booking.hotel.id = :hotelId
              AND serviceUsage.confirmedAt >= :start
              AND serviceUsage.confirmedAt < :end
            GROUP BY FUNCTION('DATE', serviceUsage.confirmedAt)
            ORDER BY FUNCTION('DATE', serviceUsage.confirmedAt)
            """)
    List<DailyServiceRevenueProjection> getDailyConfirmedServiceRevenue(
            @Param("hotelId") Long hotelId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            SELECT FUNCTION('DATE', service.usedAt) AS revenueDate,
                   COALESCE(SUM(COALESCE(service.price, 0) * service.usedQuantity), 0) AS revenue,
                   COUNT(DISTINCT service.id) AS serviceUsageCount
            FROM BookingServiceDetail service
            JOIN service.bookingDetail detail
            JOIN detail.booking booking
            WHERE booking.hotel.id = :hotelId
              AND service.usedAt >= :start
              AND service.usedAt < :end
              AND service.usedQuantity > 0
              AND NOT EXISTS (
                  SELECT existingUsage.id
                  FROM BookingServiceUsage existingUsage
                  WHERE existingUsage.bookingServiceDetail = service
              )
            GROUP BY FUNCTION('DATE', service.usedAt)
            ORDER BY FUNCTION('DATE', service.usedAt)
            """)
    List<DailyServiceRevenueProjection> getDailyLegacyServiceRevenue(
            @Param("hotelId") Long hotelId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
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
