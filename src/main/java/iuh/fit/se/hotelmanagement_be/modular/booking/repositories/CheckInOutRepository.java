package iuh.fit.se.hotelmanagement_be.modular.booking.repositories;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CheckInOutRepository {
    @Query("SELECT bd FROM BookingDetail bd " +
            "JOIN bd.room r JOIN r.floor f JOIN f.building b JOIN b.hotel h " +
            "WHERE h.id = :hotelId " +
            "AND CAST(bd.checkinTime AS localdate) = :date " +
            "AND bd.status = :status")
    List<BookingDetail> findArrivalsByHotelAndDateAndStatus(
            @Param("hotelId") Long hotelId,
            @Param("date") LocalDate date,
            @Param("status") BookingStatusType status);

    // Lọc danh sách Check-out hôm nay theo từng Khách sạn cụ thể
    @Query("SELECT bd FROM BookingDetail bd " +
            "JOIN bd.room r JOIN r.floor f JOIN f.building b JOIN b.hotel h " +
            "WHERE h.id = :hotelId " +
            "AND CAST(bd.checkoutTime AS localdate) = :date " +
            "AND bd.status = :status")
    List<BookingDetail> findDeparturesByHotelAndDateAndStatus(
            @Param("hotelId") Long hotelId,
            @Param("date") LocalDate date,
            @Param("status") BookingStatusType status);

}
