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
    CustomerRepository customerRepository;
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
        if (otpRepository.existsByPhoneAndExpiredAtAfter(request.getPhone(), now)) {
            throw new AppException(ErrorCode.PHONE_OTP_PENDING);
        }
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_EXISTED);
        }

        if (otpRepository.existsByEmailAndExpiredAtAfter(request.getEmail(), now)) {
            throw new AppException(ErrorCode.EMAIL_OTP_PENDING);
        }


        if (request.getCccd() != null && customerRepository.existsByCccd(request.getCccd())) {
            throw new AppException(ErrorCode.CCCD_EXISTED);
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
}
