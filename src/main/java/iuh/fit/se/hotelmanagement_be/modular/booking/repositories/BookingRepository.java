package iuh.fit.se.hotelmanagement_be.modular.booking.repositories;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
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


}
