package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.OtpVerification;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.OtpRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserRegisterRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {
    @Autowired
    private OtpRepository otpRepository;
    @Autowired
    private EmailService emailService;

    @Transactional
    public void saveOtp(String email, String otpCode, UserRegisterRequest request) {
        // Email này da có OTP cũ thì xóa trước khi tạo mới
        otpRepository.deleteByEmail(email);

        OtpVerification otp = OtpVerification.builder()
                .email(email)
                .otpCode(otpCode)
                .fullName(request.getFullName())
                .password(request.getPassword())
                .phone(request.getPhone())
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();
        otpRepository.save(otp);
    }


    public boolean validateOtp(String email, String inputOtp) {

        OtpVerification otp = otpRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(" Khong tim thay thong tin hop le"));

        // TH1 da xac thu thanh cong truoc do
        if (otp.isVerified())
            throw new RuntimeException(" Tai khoan da duoc xac thuc thanh cong truoc do");
        // TH2 tai khoan da bi khoa do dang nhap sai qua 3 lan
        if (otp.getFailedAttempts() > 3)
            throw new RuntimeException(" Ma OTP da nhap sai qua 3 lan. Vui long nhan yeu cau gui lai ma");
        // TH3 Ma OTP het han

        if(otp.getExpiredAt().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Ma OTP da het han vui long yeu cau gui lai");

        // TH4: kiem tra khop ma OTP
        if(otp.getOtpCode().equals(inputOtp)){
            otp.setVerified(true);
            otp.setFailedAttempts(0);
            otpRepository.save(otp);
            return true;

        }else{
            //tang so lan sai len 1
            int newAttempts = otp.getFailedAttempts() + 1;
            otp.setFailedAttempts(newAttempts);
            otpRepository.save(otp);
            if (newAttempts >= 3) {
                throw new RuntimeException("Nhập sai quá 3 lần! Mã OTP đã bị khóa. Vui lòng ấn Gửi lại mã");
            }
            throw new RuntimeException("Mã OTP không chính xác. Bạn còn " + (3 - newAttempts) + " lần thử");

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
