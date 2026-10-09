package iuh.fit.se.hotelmanagement_be.modular.room.repositories;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.BedType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BedTypeRepository extends JpaRepository<BedType, Long> {
    BedType findByName(String name);

}
