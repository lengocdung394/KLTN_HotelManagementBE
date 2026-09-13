package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.OtpVerification;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.OtpRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerCreateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {
    @Autowired
    private OtpRepository otpRepository;
    @Autowired
    private EmailService emailService;

    @Transactional
    public void saveOtp(String email, String otpCode, CustomerCreateRequest request) {
        // Email này da có OTP cũ thì xóa trước khi tạo mới
        otpRepository.deleteByEmail(email);

        OtpVerification otp = OtpVerification.builder()
                .email(email)
                .otpCode(otpCode)
                .fullName(request.getFullName())
                .password(request.getPassword())
                .phone(request.getPhone())
                .failedAttempts(0)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();
        otpRepository.save(otp);
    }
    @Autowired
    private OtpAttemptService otpAttemptService;

    public boolean validateOtp(String email, String inputOtp) {

        OtpVerification otp = otpRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.OTP_NOT_FOUND));

        if (otp.isVerified())
            throw new AppException(ErrorCode.OTP_ALREADY_VERIFIED);

        if (otp.getFailedAttempts() > 3)
            throw new AppException(ErrorCode.OTP_LOCKED);

        if (otp.getExpiredAt().isBefore(LocalDateTime.now()))
            throw new AppException(ErrorCode.OTP_EXPIRED);

        if (otp.getOtpCode().equals(inputOtp)) {
            otp.setVerified(true);
            otp.setFailedAttempts(0);
            otpRepository.save(otp);
            return true;
        } else {
            int newAttempts = otp.getFailedAttempts() + 1;

            // Update ngay xuống DB trong transaction riêng, commit độc lập
            otpAttemptService.incrementFailedAttempt(email);

            if (newAttempts >= 3) {
                throw new AppException(ErrorCode.OTP_LOCKED);
            } else if (newAttempts == 2) {
                throw new AppException(ErrorCode.OTP_INCORRECT_2_ATTEMPTS);
            } else {
                throw new AppException(ErrorCode.OTP_INCORRECT_1_ATTEMPT);
            }
        }
    }

    public OtpVerification getPendingRegistration(String email) {
        return otpRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(("Khong tim thay thong tin dang ki")));
    }

    @Transactional
    public void clearOtp(String email) {
        otpRepository.deleteByEmail(email);
    }

    public String generateOtpCode() {
        return String.format("%06d", new Random().nextInt(999999));

    }

    public String resendOtp(String email) {
        // 1. Kiểm tra xem người dùng có thông tin đăng ký đang chờ hay không
        OtpVerification pendingUser = otpRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Khong tim thay thong tin dang ki cua User"));

        // XỬ LÝ TRƯỜNG HỢP ĐÚNG: Đã xác thực thành công rồi thì KHÔNG cho gửi lại nữa
        if (pendingUser.isVerified()) {
            throw new RuntimeException("Tài khoản đã xác thực thành công, không thể gửi lại OTP nữa");
        }

        // XỬ LÝ TRƯỜNG HỢP SAI/HẾT HẠN: Tạo mã OTP mới và reset bộ đếm sai
        String newOtp = generateOtpCode();

        pendingUser.setOtpCode(newOtp);
        pendingUser.setExpiredAt(LocalDateTime.now().plusMinutes(5));
        pendingUser.setFailedAttempts(0); // Reset số lần nhập sai về 0 để họ nhập lại 3 lần mới

        otpRepository.save(pendingUser);

        // 4. Gửi email chứa OTP mới
        emailService.sendOtpEmail(email, newOtp);
        return newOtp;
    }

}
