package iuh.fit.se.hotelmanagement_be.modular.promotion.repositories;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.UserPromotionUsage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserPromotionUsageRepository extends JpaRepository<UserPromotionUsage, String> {
    interface PromotionUsageReportProjection {
        String getPromotionId();
        String getPromotionName();
        String getPromotionCode();
        Long getUsageCount();
        Long getUserCount();
        BigDecimal getSavings();
    }

    @Query("""
            SELECT p.id AS promotionId,
                   p.name AS promotionName,
                   p.code AS promotionCode,
                   COUNT(u) AS usageCount,
                   COUNT(DISTINCT u.customer.id) AS userCount,
                   COALESCE(SUM(u.booking.applyAmount), 0) AS savings
            FROM UserPromotionUsage u
            JOIN u.booking b
            JOIN Promotion p ON p.code = u.promotionCode
            WHERE b.hotel.id = :hotelId
              AND u.usedAt >= :start
              AND u.usedAt < :end
              AND (b.bookingStatus IS NULL OR b.bookingStatus NOT IN :cancelledStatuses)
            GROUP BY p.id, p.name, p.code
            ORDER BY SUM(u.booking.applyAmount) DESC
            """)
    List<PromotionUsageReportProjection> getPromotionReport(
            @Param("hotelId") Long hotelId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("cancelledStatuses") List<BookingStatus> cancelledStatuses
    );

    @Query("""
            SELECT COUNT(u)
            FROM UserPromotionUsage u
            JOIN u.booking b
            JOIN Promotion p ON p.code = u.promotionCode
            WHERE (:hotelId IS NULL OR b.hotel.id = :hotelId)
              AND u.usedAt >= :start
              AND u.usedAt < :end
              AND (b.bookingStatus IS NULL OR b.bookingStatus NOT IN :cancelledStatuses)
            """)
    long countPromotionUsagesForPeriod(
            @Param("hotelId") Long hotelId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("cancelledStatuses") List<BookingStatus> cancelledStatuses
    );

    @Query("""
            SELECT COALESCE(SUM(u.booking.applyAmount), 0)
            FROM UserPromotionUsage u
            JOIN u.booking b
            JOIN Promotion p ON p.code = u.promotionCode
            WHERE (:hotelId IS NULL OR b.hotel.id = :hotelId)
              AND u.usedAt >= :start
              AND u.usedAt < :end
              AND (b.bookingStatus IS NULL OR b.bookingStatus NOT IN :cancelledStatuses)
            """)
    BigDecimal sumPromotionSavingsForPeriod(
            @Param("hotelId") Long hotelId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("cancelledStatuses") List<BookingStatus> cancelledStatuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from UserPromotionUsage u
            where u.customer.id = :customerId
              and u.promotionCode = :promotionCode
            """)
    Optional<UserPromotionUsage> findByCustomerAndPromotionCodeForUpdate(
            @Param("customerId") String customerId,
            @Param("promotionCode") String promotionCode
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from UserPromotionUsage u
            where u.customer.id = :customerId
              and u.booking.id = :bookingId
            """)
    Optional<UserPromotionUsage> findByCustomerAndBookingForUpdate(
            @Param("customerId") String customerId,
            @Param("bookingId") String bookingId
    );
}
