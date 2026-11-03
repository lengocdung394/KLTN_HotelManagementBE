package iuh.fit.se.hotelmanagement_be.modular.auth.repositories;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    boolean existsByPhone(String phone);

    boolean existsByCccd(String cccd);

    boolean existsByEmailIgnoreCase(String email);

    // Chỉ lấy hồ sơ khách hàng đã phát sinh booking ở bất kỳ chi nhánh nào.
    @Query("SELECT DISTINCT c FROM Customer c " +
            "JOIN c.bookings b")
    List<Customer> findCustomersWithBookings();

    // Dùng Optional giúp bắt lỗi không tìm thấy thanh lịch hơn
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findFirstByPhone(String phone);

    Optional<Customer> findByCccd(String cccd);
    Optional<Customer> findFirstByCccd(String cccd);

    Optional<Customer> findByEmail(String email);
    Optional<Customer> findFirstByEmail(String email);

    Optional<Customer> findByAccount(Account account);
    Optional<Customer> findFirstByAccount(Account account);
    // 1. Tìm kiếm khách hàng theo Số điện thoại hoặc Căn cước công dân (Dùng để check trùng khi tạo mới)
    Optional<Customer> findByPhoneAndCccd(String phone, String cccd);
}
