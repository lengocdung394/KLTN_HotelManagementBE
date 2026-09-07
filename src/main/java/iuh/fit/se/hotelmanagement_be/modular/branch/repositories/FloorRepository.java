package iuh.fit.se.hotelmanagement_be.modular.branch.repositories;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Floor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FloorRepository extends JpaRepository<Floor,Long> {
    // Hoặc lấy các Floor thuộc về 1 Building cụ thể
    List<Floor> findByBuildingId(Long buildingId);
    // Lấy các Floor thuộc về Building của Hotel này
    List<Floor> findByBuilding_Hotel_Id(Long hotelId);
}
