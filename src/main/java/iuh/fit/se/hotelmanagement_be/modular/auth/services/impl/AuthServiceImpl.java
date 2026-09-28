package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.OtpVerification;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.*;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserLoginRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.VerifyOtpRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.AuthenticationResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerGetOneResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.UserResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.AuthService;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements AuthService {
    EmployeeRepository userRepository;
    CustomerRepository customerRepository;
    PasswordEncoder passwordEncoder;
    AccountRepository accountRepository;
    OtpRepository otpRepository;
    OtpService otpService;
    JwtService jwtService;
    EmailService emailService;

    final org.springframework.security.authentication.AuthenticationManager authenticationManager;
    RoleRepository roleRepository;

//    @Override
//    public void customerRegisterRequest(CustomerCreateRequest request) {
//        LocalDateTime now = LocalDateTime.now();
//        if (otpRepository.existsByPhoneAndExpiredAtAfter(request.getPhone(), now)) {
//            throw new AppException(ErrorCode.PHONE_OTP_PENDING);
//        }
//        if (accountRepository.existsByEmail(request.getEmail())) {
//            throw new AppException(ErrorCode.EMAIL_EXISTED);
//        }
//
//        if (customerRepository.existsByPhone(request.getPhone())) {
//            throw new AppException(ErrorCode.PHONE_EXISTED);
//        }
//
//        if (otpRepository.existsByEmailAndExpiredAtAfter(request.getEmail(), now)) {
//            throw new AppException(ErrorCode.EMAIL_OTP_PENDING);
//        }
//
//
//        if (request.getCccd() != null && customerRepository.existsByCccd(request.getCccd())) {
//            throw new AppException(ErrorCode.CCCD_EXISTED);
//        }
//
//        String otpCode = otpService.generateOtpCode();
//        otpService.saveOtp(request.getEmail(), otpCode, request);
//        emailService.sendOtpEmail(request.getEmail(), otpCode);
//    }
//
//
//    @Override
//    @Transactional
//    public UserResponse verifyOtpAndRegisterCustomer(VerifyOtpRequest request) {
//        boolean isValid = otpService.validateOtp(request.getEmail(), request.getOtp());
//        if (!isValid) {
//            throw new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn");
//        }
//
//        OtpVerification pendingUser = otpService.getPendingRegistration(request.getEmail());
//
//        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
//                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Không tìm thấy cấu hình quyền ROLE_CUSTOMER"));
//
//        // 1. Tạo Account trước
//        Account account = Account.builder()
//                .email(pendingUser.getEmail())
//                .password(passwordEncoder.encode(pendingUser.getPassword()))
//                .roles(Set.of(customerRole))
//                .build();
//
//        // 2. Tạo Customer giữ khóa ngoại account
//        Customer customer = Customer.builder()
//                .fullName(pendingUser.getFullName())
//                .phone(pendingUser.getPhone())
//                .cccd(pendingUser.getCccd())
//                .email(pendingUser.getEmail())
//                .account(account) // Gán account vào Customer
//                .build();
//
//        // 3. Lưu Customer (sẽ tự động Cascade lưu Account)
//        Customer savedCustomer = customerRepository.save(customer);
//
//        otpService.clearOtp(request.getEmail());
//
//        return UserResponse.builder()
//                .id(savedCustomer.getId())
//                .fullName(savedCustomer.getFullName())
//                .email(savedCustomer.getAccount().getEmail())
//                .build();
//    }

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

        String fullName = "";
        String position = "";
        String id = null;

        if (account.getEmployee() != null) {
            fullName = account.getEmployee().getFullName();
            position = account.getEmployee().getPosition();
            id = account.getEmployee().getId();
        } else if (account.getCustomer() != null) {
            fullName = account.getCustomer().getFullName();
            id = account.getCustomer().getId();
            position = "Khách hàng";
        } else {
            // Trường hợp tài khoản là Admin thuần túy (không có Employee, không có Customer)
            fullName = "Quản trị hệ thống"; // Hoặc lấy từ đâu đó
            position = "Admin";
            id = account.getId(); // Lấy ID của chính Account luôn
        }

        return AuthenticationResponse.builder()
                .id(id)
                .token(jwtToken)
                .email(account.getEmail())
                .fullName(fullName)
                .position(position)
                .build();
    }

    private Account getCurrentAccount() {
        String email = iuh.fit.se.hotelmanagement_be.config.SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Customer getCurrentCustomer() {
        Account account = getCurrentAccount();
        if (account.getCustomer() == null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        return account.getCustomer();
    }

    private iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerProfileResponse toCustomerProfileResponse(Customer customer) {
        return iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerProfileResponse.builder()
                .id(customer.getId())
                .accountId(customer.getAccount() != null ? customer.getAccount().getId() : null)
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .cccd(customer.getCccd())
                .loyaltyTier(customer.getLoyaltyTier())
                .totalSpent(customer.getTotalSpent())
                .totalBookings(customer.getTotalBookings())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerProfileResponse getMyCustomerProfile() {
        return toCustomerProfileResponse(getCurrentCustomer());
    }

    @Override
    @Transactional
    public iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerProfileResponse updateMyCustomerProfile(
            iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerUpdateProfileRequest request) {
        Customer customer = getCurrentCustomer();

        if (customer.getPhone() != null && !customer.getPhone().equals(request.getPhone())
                && customerRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_EXISTED);
        }

        if (request.getCccd() != null && !request.getCccd().isBlank()
                && (customer.getCccd() == null || !customer.getCccd().equals(request.getCccd()))
                && customerRepository.existsByCccd(request.getCccd())) {
            throw new AppException(ErrorCode.CCCD_EXISTED);
        }

        customer.setFullName(request.getFullName().trim());
        customer.setPhone(request.getPhone().trim());
        customer.setCccd(request.getCccd() != null ? request.getCccd().trim() : null);

        return toCustomerProfileResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public void changeCustomerPassword(iuh.fit.se.hotelmanagement_be.modular.auth.requests.ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        Account account = getCurrentAccount();

        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
            throw new AppException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void forgotPasswordRequest(iuh.fit.se.hotelmanagement_be.modular.auth.requests.ForgotPasswordRequest request) {
        accountRepository.findByEmail(request.getEmail().trim())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String otpCode = otpService.generateOtpCode();

        otpRepository.deleteByEmail(request.getEmail().trim());
        OtpVerification otp = OtpVerification.builder()
                .email(request.getEmail().trim())
                .otpCode(otpCode)
                .failedAttempts(0)
                .verified(false)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();
        otpRepository.save(otp);

        log.info(">>> [FORGOT PASSWORD OTP] Email: {}, OTP: {}", request.getEmail().trim(), otpCode);
        try {
            emailService.sendOtpEmail(request.getEmail().trim(), otpCode);
        } catch (Exception e) {
            log.warn(">>> Gửi email OTP thất bại: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void resetPassword(iuh.fit.se.hotelmanagement_be.modular.auth.requests.ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        OtpVerification otp = otpRepository.findByEmail(request.getEmail().trim())
                .orElseThrow(() -> new AppException(ErrorCode.OTP_NOT_FOUND));

        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        if (otp.getFailedAttempts() >= 3) {
            throw new AppException(ErrorCode.OTP_LOCKED);
        }

        if (!otp.getOtpCode().equals(request.getOtp().trim())) {
            otp.setFailedAttempts(otp.getFailedAttempts() + 1);
            otpRepository.save(otp);
            throw new RuntimeException("Mã OTP không chính xác");
        }

        Account account = accountRepository.findByEmail(request.getEmail().trim())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);

        otpRepository.deleteByEmail(request.getEmail().trim());
    }

    @Override
    public List<CustomerGetOneResponse> getAllCustomers() {
        log.info("==> [API] Đang lấy danh sách tất cả khách hàng trong hệ thống...");
        List<Customer> customers = customerRepository.findAll();
        log.info("==> [SUCCESS] Tìm thấy tổng cộng {} khách hàng.", customers.size());

        return customers.stream()
                .map(this::mapToCustomerResponse)
                .toList();
    }

    @Override
    public List<CustomerGetOneResponse> getCustomersByHotelId(Long hotelId) {
        log.info("==> [API] Đang lấy danh sách khách hàng theo chi nhánh hotelId: {}...", hotelId);
        List<Customer> customers = customerRepository.findCustomersByHotelId(hotelId);
        log.info("==> [SUCCESS] Tìm thấy {} khách hàng cho chi nhánh hotelId: {}.", customers.size(), hotelId);

        return customers.stream()
                .map(this::mapToCustomerResponse)
                .toList();
    }

    private CustomerGetOneResponse mapToCustomerResponse(Customer customer) {
        log.debug("Đang map dữ liệu cho khách hàng ID: {}", customer.getId());

        int totalBookings = (customer.getBookings() != null) ? customer.getBookings().size() : 0;
        BigDecimal totalSpent = BigDecimal.ZERO;

        if (customer.getBookings() != null) {
            for (Booking booking : customer.getBookings()) {
                if (booking.getOrder() != null && booking.getOrder().getTotalAmount() != null) {
                    totalSpent = totalSpent.add(booking.getOrder().getTotalAmount());
                }
            }
        }

        return CustomerGetOneResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .cccd(customer.getCccd())
                .loyaltyTier(customer.getLoyaltyTier())
                .totalSpent(totalSpent)
                .totalBookings(totalBookings)
                .build();
    }
}
