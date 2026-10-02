package iuh.fit.se.hotelmanagement_be.modular.auth.repositories;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    boolean existsByPhone(String phone);

    boolean existsByCccd(String cccd);

    // Lấy danh sách khách hàng theo chi nhánh hoặc khách hàng mới chưa có đặt phòng
    @Query("SELECT DISTINCT c FROM Customer c " +
            "LEFT JOIN c.bookings b " +
            "LEFT JOIN b.bookingDetails bd " +
            "LEFT JOIN bd.room r " +
            "LEFT JOIN r.floor f " +
            "LEFT JOIN f.building bu " +
            "WHERE bu.hotel.id = :hotelId OR c.bookings IS EMPTY")
    List<Customer> findCustomersByHotelId(@Param("hotelId") Long hotelId);

    // Dùng Optional giúp bắt lỗi không tìm thấy thanh lịch hơn
    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByCccd(String cccd);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByAccount(Account account);
    // 1. Tìm kiếm khách hàng theo Số điện thoại hoặc Căn cước công dân (Dùng để check trùng khi tạo mới)
    Optional<Customer> findByPhoneAndCccd(String phone, String cccd);
}
