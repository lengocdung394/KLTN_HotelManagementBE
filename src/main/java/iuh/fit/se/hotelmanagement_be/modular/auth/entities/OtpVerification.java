package iuh.fit.se.hotelmanagement_be.modular.auth.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@EqualsAndHashCode()
@Data
@SuperBuilder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "otp_verification")
public class OtpVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otpverification_id")
    Long id;

    @Column(nullable = false)
    String email;

    @Column(nullable = false)
    String otpCode;

    @Column(nullable = false)
    LocalDateTime expiredAt; // Thời điểm hết hạn (vd: now + 5 phút)

    // Thêm lưu tạm thông tin đăng ký (đã mã hóa password) hoặc dạng JSON
    String fullName;

    String phone;

    String password;

    boolean verified;

    String cccd;

    int failedAttempts;

}
