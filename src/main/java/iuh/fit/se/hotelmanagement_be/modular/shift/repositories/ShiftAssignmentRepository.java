package iuh.fit.se.hotelmanagement_be.modular.shift.repositories;

import iuh.fit.se.hotelmanagement_be.modular.shift.entities.ShiftAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, String> {

    @Query("SELECT s FROM ShiftAssignment s LEFT JOIN FETCH s.employee e WHERE s.hotel.id = :hotelId AND s.workDate = :workDate ORDER BY s.shiftType ASC")
    List<ShiftAssignment> findByHotelIdAndWorkDate(@Param("hotelId") Long hotelId, @Param("workDate") LocalDate workDate);

    @Query("SELECT s FROM ShiftAssignment s LEFT JOIN FETCH s.employee e WHERE s.hotel.id = :hotelId AND s.workDate BETWEEN :startDate AND :endDate ORDER BY s.workDate ASC, s.shiftType ASC")
    List<ShiftAssignment> findByHotelIdAndWorkDateBetween(
            @Param("hotelId") Long hotelId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<ShiftAssignment> findByHotel_IdAndWorkDateAndRoleAndShiftType(
            Long hotelId, LocalDate workDate, String role, String shiftType
    );

    List<ShiftAssignment> findByHotel_IdAndWorkDateBetweenOrderByWorkDateAsc(
            Long hotelId, LocalDate startDate, LocalDate endDate
    );
}
