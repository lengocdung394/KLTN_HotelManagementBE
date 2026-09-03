package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account; // Import thêm Account
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.OtpVerification;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.User;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository; // Inject AccountRepository
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.OtpRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.RoleRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements AuthService {
    UserRepository userRepository;
    AccountRepository accountRepository;
    PasswordEncoder passwordEncoder;
    OtpRepository otpRepository;
    OtpService otpService;
    JwtService jwtService;
    EmailService emailService;

    final org.springframework.security.authentication.AuthenticationManager authenticationManager;
    private final RoleRepository roleRepository;

    @Override
    public void customerRegisterRequest(UserRegisterRequest request) {
        LocalDateTime now = LocalDateTime.now();

        // 💡 1. Sửa lệnh check: Kiểm tra email tồn tại dưới bảng accounts thay vì users
        if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists in official accounts");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("So dien thoai nay da duoc dang ky tai khoan");
        }

        if (otpRepository.existsByEmailAndExpiredAtAfter(request.getEmail(), now)) {
            throw new RuntimeException("Email nay dang trong qua trinh cho xac thuc OTP.");
        }

        if (otpRepository.existsByPhoneAndExpiredAtAfter(request.getPhone(), now)) {
            throw new RuntimeException("So dien thoai nay dang cho xac thuc boi mot yeu cau khac.");
        }

        if (request.getCccd() != null && userRepository.existsByCccd(request.getCccd())) {
            throw new RuntimeException("Số CCCD này đã được sử dụng trong hệ thống!");
        }
        String otpCode = otpService.generateOtpCode();
        otpService.saveOtp(request.getEmail(), otpCode, request);
        emailService.sendOtpEmail(request.getEmail(), otpCode);
    }

    @Override
    @Transactional
    public UserResponse verifyOtpAndRegisterCustomer(VerifyOtpRequest request) {
        boolean isValid = otpService.validateOtp(request.getEmail(), request.getOtp());
        if (!isValid) {
            throw new RuntimeException("Invalid OTP");
        }

        OtpVerification pendingUser = otpService.getPendingRegistration(request.getEmail());

        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Không tìm thấy cấu hình quyền ROLE_CUSTOMER"));

        // LƯU CHUNG VÀO BẢNG USER: Khách hàng tự động mang chức danh "Khách hàng"
        User user = User.builder()
                .fullName(pendingUser.getFullName())
                .phone(pendingUser.getPhone())
                .cccd(pendingUser.getCccd())
                .position("Khách hàng") // Định danh phân biệt ở tầng nghiệp vụ
                .hotel(null)            // Khách hàng vãng lai không thuộc biên chế chi nhánh nào
                .build();

        Account account = Account.builder()
                .email(pendingUser.getEmail())
                .password(passwordEncoder.encode(pendingUser.getPassword()))
                .roles(Set.of(customerRole))
                .build();

        // Thiết lập mối quan hệ 1-1
        account.setUser(user);
        user.setAccount(account);

        // Lưu duy nhất bảng UserRepository
        User savedUser = userRepository.save(user);

        otpService.clearOtp(request.getEmail());

        return UserResponse.builder()
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getAccount().getEmail())
                .build();
    }


    @Override
    public AuthenticationResponse login(UserLoginRequest request) {
        try {
            // Spring Security sẽ dùng CustomUserDetailsServiceImpl để load Account lên đối chiếu pass
            authenticationManager.authenticate(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new RuntimeException("Tài khoản hoặc mật khẩu không chính xác");
        }

        // 💡 4. Đăng nhập thành công -> Lấy Account lên để làm vé JWT token
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        // Truyền thực thể account (đã implements UserDetails) vào hàm sinh token
        String jwtToken = jwtService.generateToken(account);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .email(account.getEmail())
                .fullName(account.getUser().getFullName()) // Lấy tên hiển thị từ thực thể User liên kết
                .build();
    }
}
