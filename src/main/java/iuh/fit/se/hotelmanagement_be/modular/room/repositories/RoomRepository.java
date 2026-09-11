package iuh.fit.se.hotelmanagement_be.modular.room.repositories;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    // Hoặc lấy các Room thuộc về 1 Floor cụ thể
    List<Room> findByFloorId(Long floorId);
    List<Room> findByFloor_Building_Hotel_Id(Long hotelId);
}
