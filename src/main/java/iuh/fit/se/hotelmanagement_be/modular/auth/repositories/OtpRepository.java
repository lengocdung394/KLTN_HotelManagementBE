package iuh.fit.se.hotelmanagement_be.modular.auth.repositories;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findByEmail(String email);
    void deleteByEmail(String email);

    // THÊM HÀM NÀY: Kiểm tra số điện thoại đang chờ xác thực và OTP còn hạn (chưa quá 5 phút)
    boolean existsByPhoneAndExpiredAtAfter(String phone, LocalDateTime now);

    // THÊM HÀM NÀY: Kiểm tra email đang chờ xác thực và OTP còn hạn
    boolean existsByEmailAndExpiredAtAfter(String email, LocalDateTime now);
}
