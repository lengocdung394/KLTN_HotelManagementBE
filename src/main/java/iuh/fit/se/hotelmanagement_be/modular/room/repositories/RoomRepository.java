package iuh.fit.se.hotelmanagement_be.modular.room.repositories;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomBed;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, String> {
    interface RoomStatusReportProjection {
        iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus getRoomStatus();
        Long getRoomCount();
    }

    @Query("""
            SELECT r.roomStatus AS roomStatus, COUNT(r) AS roomCount
            FROM Room r
            WHERE r.floor.building.hotel.id = :hotelId
            GROUP BY r.roomStatus
            """)
    List<RoomStatusReportProjection> getRoomStatusReport(@Param("hotelId") Long hotelId);

    // Hoặc lấy các Room thuộc về 1 Floor cụ thể
    List<Room> findByFloorId(String floorId);
    List<Room> findByFloor_Building_Hotel_Id(Long hotelId);
    List<Room> findByFloor_Building_Hotel_IdAndRoomStatus(Long hotelId, iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus roomStatus);
    List<Room> findByFloor_Building_Hotel_IdAndRoomTypeAndRoomStatus(Long hotelId, iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType roomType, iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus roomStatus);
    @Query("SELECT MAX(r.roomNumber) FROM Room r WHERE r.floor.id = :floorId")
    String findMaxRoomNumberByFloorId(@Param("floorId") String floorId);
    // 2. Hàm kiểm tra xem phòng có số `roomNumber` đã tồn tại trong `floor.id` hay chưa (Trả về true/false)
    boolean existsByFloorIdAndRoomNumber(String floorId, String roomNumber);
    boolean existsByFloorIdAndRoomNumberAndIdNot(String id, String roomNumber,String roomId);
    boolean existsByFloorIdAndRoomType(String floorId, iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType roomType);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :roomId")
    Optional<Room> findByIdWithLock(@Param("roomId") String roomId);
    Optional<Room> findByIdAndFloor_Building_Hotel_Id(
            String id,
            Long hotelId
    );
    Optional<Room> findFirstByRoomNumber(String roomNumber);
    Optional<Room> findFirstByRoomNumberAndFloor_Building_Hotel_Id(String roomNumber, Long hotelId);
}
