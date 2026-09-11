package iuh.fit.se.hotelmanagement_be.modular.booking.repositories;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {
    @Query("""
                SELECT r FROM Room r
                WHERE r.floor.building.hotel.id = :hotelId
                  AND r.roomStatus != iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus.MAINTENANCE
                  AND NOT EXISTS (
                      SELECT 1 FROM BookingDetail bd
                      WHERE bd.room.id = r.id
                        AND bd.booking.bookingStatus IN (
                            iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.CONFIRMED,
                            iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.IN_HOUSE,
                            iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus.PENDING
                        )
                        AND bd.checkinTime < :checkoutTime 
                        AND bd.checkoutTime > :checkinTime
                  )
            """)
    List<Room> findAvailableRooms(
            @Param("hotelId") Long hotelId,
            @Param("checkinTime") LocalDateTime checkinTime,
            @Param("checkoutTime") LocalDateTime checkoutTime
    );
}
