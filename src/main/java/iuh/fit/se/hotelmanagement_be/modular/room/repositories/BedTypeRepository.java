package iuh.fit.se.hotelmanagement_be.modular.room.repositories;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.BedType;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomBed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface BedTypeRepository extends JpaRepository<BedType, Long> {
    BedType findByName(String name);

    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
