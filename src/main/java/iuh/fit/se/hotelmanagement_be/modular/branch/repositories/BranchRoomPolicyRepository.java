package iuh.fit.se.hotelmanagement_be.modular.branch.repositories;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRoomPolicyRepository extends JpaRepository<BranchRoomPolicy, Long> {
    BranchRoomPolicy  findByHotelIdAndRoomType(Long hotelId, RoomType roomType);
}
