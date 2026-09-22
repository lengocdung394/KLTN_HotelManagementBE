package iuh.fit.se.hotelmanagement_be.modular.auth.repositories;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    boolean existsByPhone(String phone);

    boolean existsByCccd(String cccd);
    // Lấy danh sách khách hàng theo chi nhánh thông qua Room -> Floor -> Building -> Hotel
    @Query("SELECT DISTINCT b.customer FROM Booking b " +
            "JOIN b.bookingDetails bd " +
            "JOIN bd.room r " +
            "JOIN r.floor f " +
            "JOIN f.building bu " +
            "WHERE bu.hotel.id = :hotelId")
    List<Customer> findCustomersByHotelId(@Param("hotelId") Long hotelId);

}
