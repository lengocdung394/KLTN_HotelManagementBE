package iuh.fit.se.hotelmanagement_be.modular.room.repositories;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.BedType;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomTypeBed;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BedTypeRepository extends JpaRepository<BedType, Long> {
    BedType findByName(String name);

}
