package iuh.fit.se.hotelmanagement_be.modular.room.repositories;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomSeasonalRate;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface RoomSeasonalRateRepository extends JpaRepository<RoomSeasonalRate,Long> {
    @Query("SELECT r FROM RoomSeasonalRate r WHERE r.hotelId= :hotelId AND r.roomType = :roomType AND :currentDate BETWEEN r.startDate AND r.endDate")
    Optional<RoomSeasonalRate> findActiveRateByDate(
            @Param("hotelId") Long hotelId,
            @Param("roomType") RoomType roomType,
            @Param("currentDate") LocalDate currentDate
    );
}
