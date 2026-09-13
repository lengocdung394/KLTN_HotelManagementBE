package iuh.fit.se.hotelmanagement_be.modular.auth.repositories;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.OtpVerification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
@Repository
public interface OtpRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findByEmail(String email);
    void deleteByEmail(String email);

    // THÊM HÀM NÀY: Kiểm tra số điện thoại đang chờ xác thực và OTP còn hạn (chưa quá 5 phút)
    boolean existsByPhoneAndExpiredAtAfter(String phone, LocalDateTime now);

    // THÊM HÀM NÀY: Kiểm tra email đang chờ xác thực và OTP còn hạn
    boolean existsByEmailAndExpiredAtAfter(String email, LocalDateTime now);
    @Transactional
    @Modifying
    @Query("UPDATE OtpVerification o SET o.failedAttempts = o.failedAttempts + 1 WHERE o.email = :email")
    void incrementFailedAttempts(@Param("email") String email);
}
