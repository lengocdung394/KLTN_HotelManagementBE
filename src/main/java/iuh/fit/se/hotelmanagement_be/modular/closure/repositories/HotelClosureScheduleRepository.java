package iuh.fit.se.hotelmanagement_be.modular.closure.repositories;

import iuh.fit.se.hotelmanagement_be.modular.closure.entities.HotelClosureSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HotelClosureScheduleRepository extends JpaRepository<HotelClosureSchedule, String> {
    List<HotelClosureSchedule> findAllByActiveTrue();

    @Query("""
            select distinct schedule
            from HotelClosureSchedule schedule
            left join schedule.hotelIds targetHotelId
            where schedule.active = true
              and (schedule.allHotels = true or targetHotelId = :hotelId)
              and schedule.startDate < :checkoutDate
              and schedule.endDate >= :checkinDate
            """)
    List<HotelClosureSchedule> findBlockingSchedules(
            @Param("hotelId") Long hotelId,
            @Param("checkinDate") LocalDate checkinDate,
            @Param("checkoutDate") LocalDate checkoutDate);

    @Query("""
            select distinct schedule
            from HotelClosureSchedule schedule
            left join schedule.hotelIds targetHotelId
            where schedule.active = true
              and (schedule.allHotels = true or targetHotelId = :hotelId)
            order by schedule.startDate desc
            """)
    List<HotelClosureSchedule> findActiveForHotel(@Param("hotelId") Long hotelId);

    @Query("""
            select distinct schedule
            from HotelClosureSchedule schedule
            left join schedule.hotelIds targetHotelId
            where schedule.active = true
              and (schedule.allHotels = true or targetHotelId = :hotelId)
              and schedule.endDate >= :today
            order by schedule.startDate asc
            """)
    List<HotelClosureSchedule> findUpcomingForHotel(
            @Param("hotelId") Long hotelId,
            @Param("today") LocalDate today);

    Optional<HotelClosureSchedule> findByIdAndActiveTrue(String id);
}
