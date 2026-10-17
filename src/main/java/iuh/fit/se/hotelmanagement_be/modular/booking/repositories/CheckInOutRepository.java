package iuh.fit.se.hotelmanagement_be.modular.booking.repositories;


import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CheckInOutRepository extends JpaRepository<BookingDetail, String> {
    @Query("SELECT COUNT(DISTINCT bd.id) FROM BookingDetail bd " +
            "JOIN bd.room r JOIN r.floor f JOIN f.building b JOIN b.hotel h " +
            "WHERE (h.id = :hotelId OR bd.booking.hotel.id = :hotelId) " +
            "AND CAST(bd.checkinTime AS localdate) = :date " +
            "AND bd.status = iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType.PENDING " +
            "AND bd.booking.bookingStatus IN (" +
            "iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.CONFIRMED, " +
            "iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.IN_HOUSE)")
    long countWaitingCheckIns(@Param("hotelId") Long hotelId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(DISTINCT bd.id) FROM BookingDetail bd " +
            "JOIN bd.room r JOIN r.floor f JOIN f.building b JOIN b.hotel h " +
            "WHERE (h.id = :hotelId OR bd.booking.hotel.id = :hotelId) " +
            "AND bd.actualCheckInTime IS NOT NULL " +
            "AND bd.actualCheckInTime < :dayEnd " +
            "AND (bd.actualCheckOutTime IS NULL OR bd.actualCheckOutTime >= :dayStart) " +
            "AND bd.status IN (" +
            "iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType.CHECKED_IN, " +
            "iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType.CHECKED_OUT)")
    long countInHouseOnDate(
            @Param("hotelId") Long hotelId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);

    @Query("SELECT COUNT(DISTINCT bd.id) FROM BookingDetail bd " +
            "JOIN bd.room r JOIN r.floor f JOIN f.building b JOIN b.hotel h " +
            "WHERE (h.id = :hotelId OR bd.booking.hotel.id = :hotelId) " +
            "AND CAST(bd.checkoutTime AS localdate) = :date " +
            "AND bd.status = iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType.CHECKED_IN " +
            "AND bd.booking.bookingStatus IN (" +
            "iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.CONFIRMED, " +
            "iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.IN_HOUSE)")
    long countWaitingCheckOuts(@Param("hotelId") Long hotelId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(DISTINCT bd.id) FROM BookingDetail bd " +
            "JOIN bd.room r JOIN r.floor f JOIN f.building b JOIN b.hotel h " +
            "WHERE (h.id = :hotelId OR bd.booking.hotel.id = :hotelId) " +
            "AND CAST(bd.actualCheckOutTime AS localdate) = :date " +
            "AND bd.status = iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType.CHECKED_OUT")
    long countCompletedCheckOuts(@Param("hotelId") Long hotelId, @Param("date") LocalDate date);

    @Query("SELECT bd FROM BookingDetail bd " +
            "JOIN bd.room r JOIN r.floor f JOIN f.building b JOIN b.hotel h " +
            "WHERE (h.id = :hotelId OR bd.booking.hotel.id = :hotelId) " +
            "AND CAST(bd.checkinTime AS localdate ) = :date " +
            "AND bd.status = :status " +
            "AND bd.booking.bookingStatus = :bookingStatus")
    List<BookingDetail> findArrivalsByHotelAndDateAndStatus(
            @Param("hotelId") Long hotelId,
            @Param("date") LocalDate date,
            @Param("bookingStatus") BookingStatus bookingStatus,
            @Param("status") BookingStatusType status);

    @Query("SELECT bd FROM BookingDetail bd " +
            "JOIN bd.room r JOIN r.floor f JOIN f.building b JOIN b.hotel h " +
            "WHERE (h.id = :hotelId OR bd.booking.hotel.id = :hotelId) " +
            "AND CAST(bd.actualCheckInTime AS localdate) = :date " +
            "AND bd.actualCheckInTime < bd.checkinTime " +
            "AND bd.status IN (iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType.CHECKED_IN, " +
            "iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType.CHECKED_OUT)")
    List<BookingDetail> findEarlyCheckInsByHotelAndDate(
            @Param("hotelId") Long hotelId,
            @Param("date") LocalDate date);

    // Lọc danh sách Check-out hôm nay theo từng Khách sạn cụ thể (chấp nhận cả CONFIRMED và IN_HOUSE)
    @Query("SELECT bd FROM BookingDetail bd " +
            "JOIN bd.room r JOIN r.floor f JOIN f.building b JOIN b.hotel h " +
            "WHERE (h.id = :hotelId OR bd.booking.hotel.id = :hotelId) " +
            "AND CAST(bd.checkoutTime AS localdate ) = :date " +
            "AND bd.status = :status " +
            "AND (bd.booking.bookingStatus = :bookingStatus OR bd.booking.bookingStatus = iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.IN_HOUSE)"
    )
    List<BookingDetail> findDeparturesByHotelAndDateAndStatus(
            @Param("hotelId") Long hotelId,
            @Param("date") LocalDate date,
            @Param("bookingStatus") BookingStatus bookingStatus,
            @Param("status") BookingStatusType status);

}
