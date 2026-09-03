package iuh.fit.se.hotelmanagement_be.modular.branch.repositories;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Building;
import org.springframework.data.jpa.repository.JpaRepository;

public interface  BuildingRepository extends JpaRepository<Building, Long> {
}
