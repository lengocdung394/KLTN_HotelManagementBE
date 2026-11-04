package iuh.fit.se.hotelmanagement_be.modular.branch.repositories;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HotelRepository  extends JpaRepository<Hotel, Long> {
    boolean existsByName(String s);

    Optional<Hotel> findByName(String hotelName);
    // Ham lay ds khach san theo tinh thanh
    List<Hotel> findAllByProvince_Id(String provinceId);
    boolean existsByPhone(String phone);
    boolean existsByAddress(String address);

    @Query("select distinct hotel from Hotel hotel left join fetch hotel.province left join fetch hotel.roomPolicies")
    List<Hotel> findAllWithPublicDetails();
}
