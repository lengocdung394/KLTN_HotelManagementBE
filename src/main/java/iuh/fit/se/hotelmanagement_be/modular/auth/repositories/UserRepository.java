package iuh.fit.se.hotelmanagement_be.modular.auth.repositories;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.User;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.UserResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // THÊM PHƯƠNG THỨC NÀY ĐỂ KIỂM TRA SỐ ĐIỆN THOẠI
    boolean existsByPhone(String phone);
    boolean existsByCccd(String cccd);

    // Lấy riêng danh sách tất cả Khách hàng ngoài Web
    List<User> findByPosition(String position); // Truyền vào chữ "Khách hàng"

    // Lấy danh sách tất cả Nhân viên thuộc một khách sạn cụ thể
    List<User> findByHotelIdAndPositionNot(Long hotelId, String position); // Truyền vào hotelId và chữ "Khách hàng" để lọc bỏ khách ra
}
