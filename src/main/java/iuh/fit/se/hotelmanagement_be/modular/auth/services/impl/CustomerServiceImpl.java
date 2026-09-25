package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.OtpVerification;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.CustomerRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.OtpRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.RoleRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerCheckRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerRegisterRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.VerifyOtpRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.WalkInCustomerRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerCheckResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerFindByIdResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerRegisterResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.UserResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.CustomerService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CustomerServiceImpl implements CustomerService {
    CustomerRepository customerRepository;
    AccountRepository accountRepository;
    OtpRepository otpRepository;
    OtpService otpService;
    EmailService emailService;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;

    @Override
    public Customer createWalkInCustomer(WalkInCustomerRequest request) {
        // 1. Kiểm tra xem khách hàng đã tồn tại dựa vào SĐT hoặc CCCD chưa
        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_EXISTED);
        }
        if (customerRepository.existsByCccd(request.getCccd())) {
            throw new AppException(ErrorCode.CCCD_EXISTED);
        }

        // 2. Khởi tạo đối tượng Customer mới
        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setPhone(request.getPhone());
        customer.setCccd(request.getCccd());
        customer.setRegistered(false);
        customer.setAccount(null);
        // 3. Lưu vào Database (Mã ID dạng CUS_YYYYMMDD_XXXXXX sẽ tự động sinh nhờ @PrePersist)
        return customerRepository.save(customer);
    }

    @Override
    public CustomerFindByIdResponse getCustomerById(String id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
        return CustomerFindByIdResponse.builder()
                .name(customer.getFullName())
                .cccd(customer.getCccd())
                .phone(customer.getPhone()).build();
    }

    @Override
    public CustomerRegisterResponse customerRegisterRequest(CustomerRegisterRequest request) {
        LocalDateTime now = LocalDateTime.now();

        Customer customer = customerRepository
                .findByPhone(request.getPhone())
                .orElseGet(() ->
                        customerRepository
                                .findByCccd(request.getCccd())
                                .orElse(null)
                );

        // Khách đã có tài khoản hoàn chỉnh
        if (customer != null && customer.isRegistered()) {
            throw new AppException(ErrorCode.CUSTOMER_ALREADY_REGISTERED);
        }

        /*
         * Email đã tồn tại ở khách khác hoặc Account khác
         */
        Customer customerByEmail = customerRepository
                .findByEmail(request.getEmail())
                .orElse(null);

        if (customerByEmail != null
                && (customer == null
                || !customerByEmail.getId().equals(customer.getId()))) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        if (otpRepository.existsByEmailAndExpiredAtAfter(
                request.getEmail(), now)) {
            throw new AppException(ErrorCode.EMAIL_OTP_PENDING);
        }

        /*
         * Nếu là khách tạo tại quầy:
         * - registered = false
         * - account = null
         * - có thể đã có email hoặc chưa
         */
        String otpCode = otpService.generateOtpCode();

        otpService.saveOtp(
                request.getEmail(),
                otpCode,
                request
        );

        emailService.sendOtpEmail(
                request.getEmail(),
                otpCode
        );

        if (customer != null) {
            return CustomerRegisterResponse.builder()
                    .status("WALK_IN_CUSTOMER_NEEDS_PASSWORD")
                    .email(request.getEmail())
                    .message("Hồ sơ khách hàng đã tồn tại. Vui lòng nhập mật khẩu để hoàn tất đăng ký.")
                    .build();
        }

        return CustomerRegisterResponse.builder()
                .status("NEW_CUSTOMER")
                .email(request.getEmail())
                .message("Mã OTP đã được gửi đến email.")
                .build();
    }

    @Override
    @Transactional
    public UserResponse verifyOtpAndRegisterCustomer(
            VerifyOtpRequest request
    ) {
        boolean valid = otpService.validateOtp(
                request.getEmail(),
                request.getOtp()
        );

        if (!valid) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        OtpVerification pendingUser =
                otpService.getPendingRegistration(request.getEmail());

        Role customerRole = roleRepository
                .findByName("ROLE_CUSTOMER")
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy ROLE_CUSTOMER"
                        )
                );

        if (accountRepository.existsByEmail(pendingUser.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        Account account = Account.builder()
                .email(pendingUser.getEmail())
                .password(
                        passwordEncoder.encode(
                                pendingUser.getPassword()
                        )
                )
                .roles(Set.of(customerRole))
                .build();

        Customer customer = customerRepository
                .findByPhone(pendingUser.getPhone())
                .orElseGet(() ->
                        customerRepository
                                .findByCccd(pendingUser.getCccd())
                                .orElse(null)
                );

        if (customer != null) {
            if (customer.isRegistered()) {
                throw new AppException(
                        ErrorCode.CUSTOMER_ALREADY_REGISTERED
                );
            }

            customer.setEmail(pendingUser.getEmail());
            customer.setAccount(account);
            customer.setRegistered(true);
        } else {
            customer = Customer.builder()
                    .fullName(pendingUser.getFullName())
                    .phone(pendingUser.getPhone())
                    .cccd(pendingUser.getCccd())
                    .email(pendingUser.getEmail())
                    .account(account)
                    .isRegistered(true)
                    .build();
        }

        Customer savedCustomer =
                customerRepository.save(customer);

        otpService.clearOtp(request.getEmail());

        return UserResponse.builder()
                .id(savedCustomer.getId())
                .fullName(savedCustomer.getFullName())
                .email(savedCustomer.getEmail())
                .build();
    }

    @Override
    public CustomerCheckResponse checkCustomer(CustomerCheckRequest request) {
        Customer customer = customerRepository
                .findByPhone(request.getPhone())
                .orElseGet(() ->
                        customerRepository
                                .findByCccd(request.getCccd())
                                .orElse(null)
                );

        if (customer != null && customer.isRegistered()) {
            return CustomerCheckResponse.builder()
                    .status("ALREADY_REGISTERED")
                    .email(customer.getEmail())
                    .message("Khách hàng đã đăng ký tài khoản.")
                    .build();
        }

        if (customer != null) {
            return CustomerCheckResponse.builder()
                    .status("WALK_IN_CUSTOMER_NEEDS_ACCOUNT")
                    .email(customer.getEmail())
                    .message("Hồ sơ khách hàng đã tồn tại. Vui lòng bổ sung email và mật khẩu.")
                    .build();
        }

        return CustomerCheckResponse.builder()
                .status("NEW_CUSTOMER")
                .message("Có thể tiếp tục nhập email và mật khẩu.")
                .build();
    }
}

