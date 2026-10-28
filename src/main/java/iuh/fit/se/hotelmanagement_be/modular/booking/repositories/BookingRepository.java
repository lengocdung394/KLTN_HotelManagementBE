package iuh.fit.se.hotelmanagement_be.modular.booking.repositories;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, String> {
    interface RoomTypeBookingReportProjection {
        RoomType getRoomType();
        Long getBookingCount();
    }

    @Query("""
            SELECT bd.room.roomType AS roomType, COUNT(bd) AS bookingCount
            FROM Booking b
            JOIN b.bookingDetails bd
            WHERE b.hotel.id = :hotelId
              AND b.createdAt >= :start
              AND b.createdAt < :end
              AND (b.bookingStatus IS NULL OR b.bookingStatus NOT IN :cancelledBookingStatuses)
              AND (bd.status IS NULL OR bd.status <> :cancelledDetailStatus)
            GROUP BY bd.room.roomType
            """)
    List<RoomTypeBookingReportProjection> getRoomTypeBookingReport(
            @Param("hotelId") Long hotelId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("cancelledBookingStatuses") List<BookingStatus> cancelledBookingStatuses,
            @Param("cancelledDetailStatus") BookingStatusType cancelledDetailStatus
    );

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
