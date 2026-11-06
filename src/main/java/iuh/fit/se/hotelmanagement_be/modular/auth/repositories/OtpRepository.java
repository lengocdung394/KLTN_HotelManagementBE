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
    Optional<OtpVerification> findByEmailIgnoreCaseAndFullNameIsNotNullAndPhoneIsNotNullAndCccdIsNotNullAndPasswordIsNotNull(
            String email);
    void deleteByEmail(String email);
    void deleteByEmailIgnoreCase(String email);

    // THÊM HÀM NÀY: Kiểm tra số điện thoại đang chờ xác thực và OTP còn hạn (chưa quá 5 phút)
    boolean existsByPhoneAndExpiredAtAfter(String phone, LocalDateTime now);

    // THÊM HÀM NÀY: Kiểm tra email đang chờ xác thực và OTP còn hạn
    boolean existsByEmailAndExpiredAtAfter(String email, LocalDateTime now);

    @Query("""
            SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
            FROM OtpVerification o
            WHERE LOWER(o.email) = LOWER(:email)
              AND o.fullName IS NOT NULL
              AND o.phone IS NOT NULL
              AND o.cccd IS NOT NULL
              AND o.password IS NOT NULL
              AND o.expiredAt > :now
            """)
    boolean existsPendingRegistrationByEmail(
            @Param("email") String email, @Param("now") LocalDateTime now);
    @Transactional
    @Modifying
    @Query("UPDATE OtpVerification o SET o.failedAttempts = o.failedAttempts + 1 WHERE LOWER(o.email) = LOWER(:email)")
    void incrementFailedAttempts(@Param("email") String email);
}
