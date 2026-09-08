package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.*;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.*;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserLoginRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserRegisterRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.VerifyOtpRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.AuthenticationResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.UserResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.AuthService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements AuthService {
    EmployeeRepository userRepository;
    CustomerRepository  customerRepository;
    PasswordEncoder passwordEncoder;
    AccountRepository accountRepository;
    OtpRepository otpRepository;
    OtpService otpService;
    JwtService jwtService;
    EmailService emailService;

    final org.springframework.security.authentication.AuthenticationManager authenticationManager;
    RoleRepository roleRepository;

    @Override
    public void customerRegisterRequest(CustomerCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();

        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        if (customerRepository.existsByPhone((request.getPhone()))) {
            throw new RuntimeException("Số điện thoại này đã được đăng ký tài khoản!");
        }

        if (otpRepository.existsByEmailAndExpiredAtAfter(request.getEmail(), now)) {
            throw new RuntimeException("Email này đang trong quá trình chờ xác thực OTP.");
        }

        if (otpRepository.existsByPhoneAndExpiredAtAfter(request.getPhone(), now)) {
            throw new RuntimeException("Số điện thoại này đang chờ xác thực bởi một yêu cầu khác.");
        }

        if (request.getCccd() != null && customerRepository.existsByCccd((request.getCccd()))) {
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
            throw new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn");
        }

        OtpVerification pendingUser = otpService.getPendingRegistration(request.getEmail());

        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Không tìm thấy cấu hình quyền ROLE_CUSTOMER"));

        // 1. Tạo Account trước
        Account account = Account.builder()
                .email(pendingUser.getEmail())
                .password(passwordEncoder.encode(pendingUser.getPassword()))
                .roles(Set.of(customerRole))
                .build();

        // 2. Tạo Customer giữ khóa ngoại account
        Customer customer = Customer.builder()
                .fullName(pendingUser.getFullName())
                .phone(pendingUser.getPhone())
                .cccd(pendingUser.getCccd())
                .email(pendingUser.getEmail())
                .account(account) // Gán account vào Customer
                .build();

        // 3. Lưu Customer (sẽ tự động Cascade lưu Account)
        Customer savedCustomer = customerRepository.save(customer);

        otpService.clearOtp(request.getEmail());

        return UserResponse.builder()
                .id(savedCustomer.getId())
                .fullName(savedCustomer.getFullName())
                .email(savedCustomer.getAccount().getEmail())
                .build();
    }

    @Override
    public AuthenticationResponse login(UserLoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS); // Hoặc RuntimeException
        }

        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        String jwtToken = jwtService.generateToken(account);

        // Xác định thông tin hiển thị (Employee hay Customer)
        String fullName = "";
        String position = "";

        if (account.getEmployee() != null) {
            fullName = account.getEmployee().getFullName();
            position = account.getEmployee().getPosition();
        } else if (account.getCustomer() != null) {
            fullName = account.getCustomer().getFullName();
            position = "Khách hàng";
        }

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .email(account.getEmail())
                .fullName(fullName)
                .position(position)
                .build();
    }
}
