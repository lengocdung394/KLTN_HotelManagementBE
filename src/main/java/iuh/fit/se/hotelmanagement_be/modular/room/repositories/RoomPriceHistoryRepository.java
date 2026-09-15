package iuh.fit.se.hotelmanagement_be.modular.room.repositories;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomPriceHistory;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomSeasonalRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomPriceHistoryRepository  extends JpaRepository<RoomPriceHistory,Long> {

    // Lấy lịch sử thay đổi giá của một khung giá cụ thể (xếp từ mới nhất về cũ nhất)
    List<RoomPriceHistory> findBySeasonalRateOrderByChangedAtDesc(RoomSeasonalRate seasonalRate);
}
