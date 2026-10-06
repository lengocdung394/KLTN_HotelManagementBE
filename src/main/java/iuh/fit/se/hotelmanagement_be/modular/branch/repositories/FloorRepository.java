package iuh.fit.se.hotelmanagement_be.modular.branch.repositories;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Floor;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FloorRepository extends JpaRepository<Floor,String> {
    // Hoặc lấy các Floor thuộc về 1 Building cụ thể
    List<Floor> findByBuildingId(String buildingId);
    // Lấy các Floor thuộc về Building của Hotel này
    List<Floor> findByBuilding_Hotel_Id(Long hotelId);
    Optional<Floor> findById(String floorId);
    Optional<Floor> findByBuildingNameAndFloorNumber(String buildingName, int floorNumber);

    boolean existsByBuilding_IdAndFloorNumber(String buildingId, int floorNumber);
    Optional<Floor> findByIdAndBuilding_Hotel_Id(String floorId, Long hotelId);
}
