package iuh.fit.se.hotelmanagement_be.modular.service.repositories;

import iuh.fit.se.hotelmanagement_be.modular.service.entities.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findAllByActiveTrue();

    List<Service> findAllByHotelId(Long hotelId);

    List<Service> findAllByCategoryIgnoreCaseAndActiveTrue(String category);

    @Query("SELECT s FROM Service s WHERE s.active = true AND (s.hotel IS NULL OR s.hotel.id = :hotelId)")
    List<Service> findAvailableServicesForHotel(@Param("hotelId") Long hotelId);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
