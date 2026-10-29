package iuh.fit.se.hotelmanagement_be.modular.booking.repositories;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, String> {
    @Query("""
                SELECT r FROM Room r
                WHERE r.floor.building.hotel.id = :hotelId
                  AND r.roomStatus != iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus.MAINTENANCE
                  AND r.roomStatus != iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus.CLEANING
                  AND NOT EXISTS (
                      SELECT 1 FROM BookingDetail bd
                      WHERE bd.room.id = r.id
                        AND bd.booking.bookingStatus IN (
                            iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.CONFIRMED,
                            iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.IN_HOUSE,
                            iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.PENDING
                        )
                        AND bd.status NOT IN (
                            iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType.CHECKED_OUT,
                            iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType.CANCELLED
                        )
                        AND (bd.booking.bookingStatus <> iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.PENDING
                             OR bd.booking.createdAt >= :fiveMinutesAgo)
                        AND bd.checkinTime < :checkoutTime 
                        AND bd.checkoutTime > :checkinTime
                  )
            """)
    List<Room> findAvailableRooms(
            @Param("hotelId") Long hotelId,
            @Param("checkinTime") LocalDateTime checkinTime,
            @Param("checkoutTime") LocalDateTime checkoutTime,
            @Param("fiveMinutesAgo") LocalDateTime fiveMinutesAgo
    );

    @Query("""
            SELECT bd FROM BookingDetail bd
            JOIN bd.booking b
            WHERE b.hotel.id = :hotelId
              AND b.bookingStatus NOT IN (
                  iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.CANCELLED,
                  iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.CANCELLED_BY_CUSTOMER,
                  iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.CANCELLED_BY_STAFF,
                  iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.CANCELLED_PAYMENT_TIMEOUT
              )
              AND (b.bookingStatus <> iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.PENDING
                   OR b.createdAt >= :fiveMinutesAgo)
              AND bd.status NOT IN (
                  iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType.CANCELLED,
                  iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType.CHECKED_OUT
              )
              AND bd.checkinTime <= :endDate
              AND bd.checkoutTime >= :startDate
            """)
    List<BookingDetail> findCalendarBookingsByDateRange(
            @Param("hotelId") Long hotelId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("fiveMinutesAgo") LocalDateTime fiveMinutesAgo
    );


    @Query("SELECT COUNT(bd) > 0 FROM BookingDetail bd " +
            "WHERE bd.room.id = :roomId " +
            "AND bd.id != :currentDetailId " +
            "AND bd.status IN ('CHECKED_IN', 'PENDING') " + // Hoặc tuỳ trạng thái active bạn muốn chặn
            "AND (CASE WHEN bd.actualCheckInTime IS NOT NULL THEN bd.actualCheckInTime ELSE bd.checkinTime END) < :checkoutTime " +
            "AND bd.checkoutTime > :effectiveCheckIn")
    boolean existsOverlappingActiveBooking(String roomId, LocalDateTime effectiveCheckIn, LocalDateTime checkoutTime, String currentDetailId);
    boolean existsByRoom_IdAndStatus(String roomId, BookingStatusType status);
    @Query("""
            SELECT COUNT(bd) FROM BookingDetail bd
            WHERE bd.room.id = :roomId
              AND bd.status NOT IN (
                  iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType.CHECKED_OUT,
                  iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType.CANCELLED
              )
              AND bd.booking.bookingStatus IN (
                  iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.PENDING,
                  iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.CONFIRMED,
                  iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.IN_HOUSE
              )
              AND (bd.booking.bookingStatus <> iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.PENDING
                   OR bd.booking.createdAt >= :fiveMinutesAgo)
              AND bd.checkinTime < :checkOut
              AND bd.checkoutTime > :checkIn
            """)
    long countOverlappingBookings(@Param("roomId") String roomId,
                                  @Param("checkIn") LocalDateTime checkIn,
                                  @Param("checkOut") LocalDateTime checkOut,
                                  @Param("fiveMinutesAgo") LocalDateTime fiveMinutesAgo);
    // Thêm hàm này vào repository của bạn
    List<BookingDetail> findByStatusAndCheckoutTimeBefore(BookingStatusType status, LocalDateTime time);
}
