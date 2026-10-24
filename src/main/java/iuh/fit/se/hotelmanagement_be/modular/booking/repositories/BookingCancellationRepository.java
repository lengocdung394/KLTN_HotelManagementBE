package iuh.fit.se.hotelmanagement_be.modular.booking.repositories;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingCancellation;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingCancellationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface BookingCancellationRepository extends JpaRepository<BookingCancellation, Long> {
    interface CancellationReportProjection {
        String getBookingId();
        BookingChannel getBookingChannel();
        String getReason();
        LocalDateTime getCancelledAt();
    }

    @Query("""
            SELECT booking.id AS bookingId,
                   booking.bookingChannel AS bookingChannel,
                   cancellation.reason AS reason,
                   COALESCE(cancellation.cancelledAt, cancellation.requestedAt) AS cancelledAt
            FROM Booking booking
            LEFT JOIN BookingCancellation cancellation ON cancellation.booking = booking
            WHERE booking.hotel.id = :hotelId
              AND booking.bookingStatus = :cancelledStatus
              AND COALESCE(cancellation.cancelledAt, cancellation.requestedAt, booking.createdAt) >= :start
              AND COALESCE(cancellation.cancelledAt, cancellation.requestedAt, booking.createdAt) < :end
            ORDER BY COALESCE(cancellation.cancelledAt, cancellation.requestedAt, booking.createdAt) DESC
            """)
    List<CancellationReportProjection> getCancellationReport(
            @Param("hotelId") Long hotelId,
            @Param("cancelledStatus") BookingStatus cancelledStatus,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    Optional<BookingCancellation> findByBookingId(String bookingId);

    @Query("select cancellation from BookingCancellation cancellation where cancellation.booking.hotel.id = :hotelId")
    List<BookingCancellation> findAllByBookingHotelId(@Param("hotelId") Long hotelId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cancellation from BookingCancellation cancellation where cancellation.booking.id = :bookingId")
    Optional<BookingCancellation> findByBookingIdForUpdate(@Param("bookingId") String bookingId);

    List<BookingCancellation> findAllByStatusOrderByRequestedAtAsc(BookingCancellationStatus status);

    @Query("""
            select cancellation
            from BookingCancellation cancellation
            where cancellation.booking.hotel.id = :hotelId
              and cancellation.status = :status
            order by cancellation.requestedAt asc
            """)
    List<BookingCancellation> findAllByHotelIdAndStatus(
            @Param("hotelId") Long hotelId,
            @Param("status") BookingCancellationStatus status);

    @Query("""
            select cancellation
            from BookingCancellation cancellation
            where cancellation.booking.hotel.id = :hotelId
            order by cancellation.requestedAt desc
            """)
    List<BookingCancellation> findAllByHotelIdOrderByRequestedAtDesc(@Param("hotelId") Long hotelId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cancellation from BookingCancellation cancellation where cancellation.id = :id")
    Optional<BookingCancellation> findByIdForUpdate(@Param("id") String id);
}
