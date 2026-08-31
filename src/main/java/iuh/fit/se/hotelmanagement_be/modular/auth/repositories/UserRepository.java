package iuh.fit.se.hotelmanagement_be.modular.auth.repositories;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.User;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.UserResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    // THÊM PHƯƠNG THỨC NÀY ĐỂ KIỂM TRA SỐ ĐIỆN THOẠI
    boolean existsByPhone(String phone);
}
