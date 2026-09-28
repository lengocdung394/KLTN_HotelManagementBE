package iuh.fit.se.hotelmanagement_be.modular.branch.repositories;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface BranchRoomPolicyRepository extends JpaRepository<BranchRoomPolicy, String> {
    BranchRoomPolicy  findByHotelIdAndRoomType(Long hotelId, RoomType roomType);
    Optional<BranchRoomPolicy> findByHotelAndRoomType(Hotel hotel, RoomType roomType);
    List<BranchRoomPolicy> findByHotelId(Long hotelId);
    // Tìm 1 chính sách cụ thể theo ID và thuộc về chi nhánh nào đó
    Optional<BranchRoomPolicy> findByIdAndHotelId(String id, Long hotelId);
}
