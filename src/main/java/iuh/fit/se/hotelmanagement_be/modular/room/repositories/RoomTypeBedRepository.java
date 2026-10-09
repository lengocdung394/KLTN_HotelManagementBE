package iuh.fit.se.hotelmanagement_be.modular.room.repositories;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomBed;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomTypeBedRepository extends JpaRepository<RoomBed, Long> {
    // 1. Tìm danh sách giường dựa vào loại phòng thông qua bảng Room liên kết
    @Query("SELECT rb FROM RoomBed rb WHERE rb.room.roomType = :roomType")
    List<RoomBed> findByRoomType(@Param("roomType") RoomType roomType);

    // 2. Kiểm tra sự tồn tại của loại giường trong một loại phòng cụ thể
    @Query("SELECT COUNT(rb) > 0 FROM RoomBed rb WHERE rb.room.roomType = :roomType AND rb.bedType.id = :bedTypeId")
    boolean existsByRoomTypeAndBedTypeId(@Param("roomType") RoomType roomType, @Param("bedTypeId") Long bedTypeId);

    // Bổ sung thêm hàm tìm theo ID phòng cụ thể (vì mỗi phòng giờ tự cấu hình giường riêng)
    @Query("SELECT rb FROM RoomBed rb WHERE rb.room.id = :roomId")
    List<RoomBed> findByRoomId(@Param("roomId") String roomId);
}
