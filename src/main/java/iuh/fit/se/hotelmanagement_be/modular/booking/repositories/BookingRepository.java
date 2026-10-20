package iuh.fit.se.hotelmanagement_be.modular.booking.repositories;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, String> {
    @Query("""
            SELECT COUNT(b)
            FROM Booking b
            WHERE b.hotel.id = :hotelId
              AND b.createdAt >= :start
              AND b.createdAt < :end
            """)
    long countCreatedBookingsForPeriod(
            @Param("hotelId") Long hotelId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    interface PromotionReportProjection {
        String getPromotionId();
        String getPromotionName();
        String getPromotionCode();
        Long getUsageCount();
        BigDecimal getSavings();
    }

    @Query("""
            SELECT COALESCE(p.id, cp.promotion.id) AS promotionId,
                   COALESCE(p.name, cp.promotion.name) AS promotionName,
                   COALESCE(p.code, cp.promotion.code) AS promotionCode,
                   COUNT(b) AS usageCount,
                   COALESCE(SUM(b.applyAmount), 0) AS savings
            FROM Booking b
            LEFT JOIN b.promotion p
            LEFT JOIN b.customerPromotion cp
            WHERE b.hotel.id = :hotelId
              AND b.applyAt >= :start
              AND b.applyAt < :end
              AND (b.bookingStatus IS NULL OR b.bookingStatus <> :cancelledStatus)
              AND (b.promotion IS NOT NULL OR b.customerPromotion IS NOT NULL)
            GROUP BY COALESCE(p.id, cp.promotion.id),
                     COALESCE(p.name, cp.promotion.name),
                     COALESCE(p.code, cp.promotion.code)
            ORDER BY SUM(b.applyAmount) DESC
            """)
    List<PromotionReportProjection> getPromotionReport(
            @Param("hotelId") Long hotelId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("cancelledStatus") BookingStatus cancelledStatus
    );

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE (:hotelId IS NULL OR b.hotel.id = :hotelId)
              AND b.applyAt >= :start
              AND b.applyAt < :end
              AND (b.promotion IS NOT NULL OR b.customerPromotion IS NOT NULL)
            """)
    long countPromotionUsagesForPeriod(
            @Param("hotelId") Long hotelId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            SELECT COALESCE(SUM(b.applyAmount), 0) FROM Booking b
            WHERE (:hotelId IS NULL OR b.hotel.id = :hotelId)
              AND b.applyAt >= :start
              AND b.applyAt < :end
              AND (b.promotion IS NOT NULL OR b.customerPromotion IS NOT NULL)
            """)
    BigDecimal sumPromotionSavingsForPeriod(
            @Param("hotelId") Long hotelId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Lọc danh sách booking theo khách sạn (hotelId) thông qua chuỗi quan hệ phòng -> tầng -> tòa nhà -> khách sạn
    @Query("""
                SELECT DISTINCT b FROM Booking b
                JOIN b.bookingDetails bd
                JOIN bd.room r
                JOIN r.floor f
                JOIN f.building bu
                JOIN bu.hotel h
                WHERE h.id = :hotelId
            """)
    List<Booking> findAllByHotelId(@Param("hotelId") Long hotelId);

    @Query("""
                select b
                from Booking b
                where b.bookingStatus = :status
                  and b.createdAt < :threshold
            """)
    List<Booking> findExpiredPendingBookings(@Param("threshold") LocalDateTime threshold, @Param("status") BookingStatus status);
    // ham tim kiem booking de phuc vu huy booking
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select booking from Booking booking where booking.id = :bookingId")
    Optional<Booking> findByIdForUpdate(@Param("bookingId") String bookingId);

}
