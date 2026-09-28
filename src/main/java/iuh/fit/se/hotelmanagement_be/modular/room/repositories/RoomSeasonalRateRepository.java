package iuh.fit.se.hotelmanagement_be.modular.room.repositories;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomSeasonalRate;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomSeasonalRateRepository extends JpaRepository<RoomSeasonalRate, Long> {
    @Query("SELECT r FROM RoomSeasonalRate r WHERE r.hotelId= :hotelId AND r.roomType = :roomType AND :currentDate BETWEEN r.startDate AND r.endDate")
    Optional<RoomSeasonalRate> findActiveRateByDate(
            @Param("hotelId") Long hotelId,
            @Param("roomType") RoomType roomType,
            @Param("currentDate") LocalDate currentDate
    );


    // Lọc khung giá theo chi nhánh và khoảng thời gian giao nhau
    @Query("SELECT r FROM RoomSeasonalRate r WHERE r.hotelId = :hotelId " +
            "AND r.startDate <= :endDate AND r.endDate >= :startDate")
    List<RoomSeasonalRate> findRatesByBranchAndDateRange(
            @Param("hotelId") Long hotelId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // lay ds su kien gia lay theo tu nho den lon  va cong phan trang

    @Query("SELECT r FROM RoomSeasonalRate r WHERE r.hotelId = :hotelId " +
            "AND (:roomType IS NULL OR r.roomType = :roomType) " +
            "AND :targetDate BETWEEN r.startDate AND r.endDate")
    Page<RoomSeasonalRate> findActiveRatesByDate(
            @Param("hotelId") Long hotelId,
            @Param("roomType") RoomType roomType,
            @Param("targetDate") LocalDate targetDate,
            Pageable pageable
    );

    @Query("SELECT COUNT(r) > 0 FROM RoomSeasonalRate r WHERE r.hotelId = :hotelId " +
            "AND r.roomType = :roomType " +
            "AND r.startDate <= :endDate AND r.endDate >= :startDate")
    boolean existsOverlappingRate(
            @Param("hotelId") Long hotelId,
            @Param("roomType") RoomType roomType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query("SELECT COUNT(r) > 0 FROM RoomSeasonalRate r WHERE r.hotelId = :hotelId AND r.roomType = :roomType AND r.id != :id AND r.startDate <= :endDate AND r.endDate >= :startDate")
    boolean existsOverlappingRateExcludingId(@Param("hotelId") Long hotelId,
                                             @Param("roomType") RoomType roomType,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             @Param("id") Long id);


    @Query("SELECT r FROM RoomSeasonalRate r WHERE r.hotelId = :hotelId AND r.startDate <= :endDateOfMonth AND r.endDate >= :startDateOfMonth")
    List<RoomSeasonalRate> findRatesByMonth(
            @Param("hotelId") Long hotelId,
            @Param("startDateOfMonth") LocalDate startDateOfMonth,
            @Param("endDateOfMonth") LocalDate endDateOfMonth
    );
}
