package iuh.fit.se.hotelmanagement_be.modular.branch.repositories;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchRoomPolicyRepository extends JpaRepository<BranchRoomPolicy, Long> {
    BranchRoomPolicy  findByHotelIdAndRoomType(Long hotelId, RoomType roomType);
    Optional<BranchRoomPolicy> findByHotelAndRoomType(Hotel hotel, RoomType roomType);
}
