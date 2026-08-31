package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.OtpVerification;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.User;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.OtpRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.UserRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserLoginRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserRegisterRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.VerifyOtpRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.AuthenticationResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.UserResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.AuthService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements AuthService {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    OtpRepository otpRepository;
    OtpService otpService;
    JwtService jwtService;
    EmailService emailService;
    // Thêm AuthenticationManager vào thuộc tính để Spring Security tự động kiểm tra mật khẩu
    final org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @Override
    public void registerRequest(UserRegisterRequest request) {
        LocalDateTime now = LocalDateTime.now();

        // ================= TẦNG 1: KIỂM TRA TRONG BẢNG USER CHÍNH THỨC =================
        // 1. Kiểm tra Email đã tài khoản chính thức chưa
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists in official accounts");
        }

        // 2. Kiểm tra Số điện thoại đã có tài khoản chính thức chưa
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("So dien thoai nay da duoc dang ky tai khoan");
        }

        // ================= TẦNG 2: KIỂM TRA TRONG BẢNG OTP TẠM (CÒN HẠN) =================
        // 3. Kiểm tra xem Email này có đang trong 5 phút chờ xác thực của một lượt bấm trước không
        if (otpRepository.existsByEmailAndExpiredAtAfter(request.getEmail(), now)) {
            throw new RuntimeException("Email nay dang trong qua trinh cho xac thuc OTP. Vui long kiem tra hop thu hoặc bấm Gửi lại mã");
        }

        // 4. Kiểm tra xem Số điện thoại này có đang bị một Email khác "giữ chỗ ảo" trong vòng 5 phút không
        if (otpRepository.existsByPhoneAndExpiredAtAfter(request.getPhone(), now)) {
            throw new RuntimeException("So dien thoai nay dang cho xac thuc boi mot yeu cau khac. Vui long thu lai sau it phut");
        }

        // ================= HẾT BỊ TRÙNG -> TIẾN HÀNH TẠO MÃ MỚI =================
        // 5. Tạo mã otp 6 chữ số
        String otpCode = otpService.generateOtpCode();

        // 6. Lưu thông tin tạm vào bảng OTP (Hàm saveOtp của bạn đã có lệnh delete OTP cũ của Email này nên rất an toàn)
        otpService.saveOtp(request.getEmail(), otpCode, request);

        // 7. Gửi email OTP
        emailService.sendOtpEmail(request.getEmail(), otpCode);

    }

    @Override
    public UserResponse verifyOtpAndRegister(VerifyOtpRequest request) {
        // 1. Validation ma OTP
        boolean isValid = otpService.validateOtp(request.getEmail(), request.getOtp());
        if (!isValid) {
            throw new RuntimeException("Invalid OTP");
        }

        // 2. Lay thong tin nguoi dung dang cho dang ki trong bang OTP tam
        OtpVerification pendingUser = otpService.getPendingRegistration(request.getEmail());

        // 3. Tao va luu User chinh thuc vao database
        User user = User.builder()
//                .id(pendingUser.getId())
                .email(pendingUser.getEmail())
                .password(passwordEncoder.encode(pendingUser.getPassword()))
                .fullName(pendingUser.getFullName())
                .phone(pendingUser.getPhone())
                .build();


        User savedUser = userRepository.save(user); // chinh thuc tao User luu vao bang

        // 4. Xoa OTP tam
        otpService.clearOtp(request.getEmail());

        // 5. Tra thong tin User moi tao
        return UserResponse.builder()
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .build();

    }

    @Override
    public AuthenticationResponse login(UserLoginRequest request) {


        try {
            // 1. Giao pho cho spring security kiem tra xem email va mat khau co khop voi DB khong
            authenticationManager.authenticate(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new RuntimeException("Tài khoản hoặc mật khẩu không chính xác");
        }
        // 2. Nếu mật khẩu đúng, lấy thông tin User lên để chuẩn bị làm vé JWT
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // 3. Tiến hành sinh chuỗi JWT Token thông qua JwtService
        String jwtToken = jwtService.generateToken(user); // Class User của bạn phải implements UserDetails

        // 4. Đóng gói chuỗi Token gửi về cho Frontend
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }

}
