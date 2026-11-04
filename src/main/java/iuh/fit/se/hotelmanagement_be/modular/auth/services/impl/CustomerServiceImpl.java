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
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.*;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.CustomerService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
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
    CustomerSocketEmitter customerSocketEmitter;
    AccountSocketEmitter accountSocketEmitter;

    @Override
    public CustomerResponse createWalkInCustomer(WalkInCustomerRequest request, Long hotelId) {
        Customer existingCustomer = findMatchingCustomer(request.getPhone(), request.getCccd());
        if (existingCustomer != null) {
            return toCustomerResponse(existingCustomer, true);
        }

        // 2. Tạo mới khách hàng
        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setPhone(request.getPhone());
        customer.setCccd(request.getCccd());
        customer.setEmail(request.getEmail() == null || request.getEmail().isBlank() ? null : request.getEmail().trim());
        customer.setRegistered(false);
        customer.setAccount(null);


        Customer savedCustomer = customerRepository.save(customer);

        accountSocketEmitter.emitGuestCustomerCreated(savedCustomer.getId(), hotelId);

        return toCustomerResponse(savedCustomer, false);
    }

    private CustomerResponse toCustomerResponse(Customer customer, boolean alreadyExists) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .cccd(customer.getCccd())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .alreadyExists(alreadyExists)
                .registered(customer.isRegistered())
                .build();
    }

    @Override
    public CustomerFindByIdResponse getCustomerById(String id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
        return CustomerFindByIdResponse.builder()
                .name(customer.getFullName())
                .cccd(customer.getCccd())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .registered(customer.isRegistered())
                .build();
    }

    @Override
    public CustomerRegisterResponse customerRegisterRequest(CustomerRegisterRequest request) {
        LocalDateTime now = LocalDateTime.now();
        request.setEmail(request.getEmail().trim().toLowerCase(java.util.Locale.ROOT));

        Customer customer = findMatchingCustomer(request.getPhone(), request.getCccd());

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

        if (otpRepository.existsPendingRegistrationByEmail(request.getEmail(), now)) {
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
                .findByCode("ROLE_CUSTOMER")
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

        Customer customer = findMatchingCustomer(
                pendingUser.getPhone(),
                pendingUser.getCccd()
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

        // ban socket khi ma tao tai khoan
        accountSocketEmitter.emitAccountCreated(
                savedCustomer.getAccount().getId(),
                savedCustomer.getId(),
                "CUSTOMER",
                null);
        otpService.clearOtp(request.getEmail());

        return UserResponse.builder()
                .id(savedCustomer.getId())
                .fullName(savedCustomer.getFullName())
                .email(savedCustomer.getEmail())
                .build();
    }

    @Override
    public CustomerCheckResponse checkCustomer(CustomerCheckRequest request) {
        Optional<Customer> matchingCustomer =
                customerRepository.findByPhoneAndCccd(request.getPhone(), request.getCccd());
        if (matchingCustomer.isEmpty()
                && hasCustomerWithPhoneOrCccd(request.getPhone(), request.getCccd())) {
            return CustomerCheckResponse.builder()
                    .status("IDENTITY_MISMATCH")
                    .message(ErrorCode.CUSTOMER_IDENTITY_MISMATCH.getMessage())
                    .build();
        }

        Customer customer = matchingCustomer.orElse(null);

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

    private Customer findMatchingCustomer(String phone, String cccd) {
        Optional<Customer> matchingCustomer =
                customerRepository.findByPhoneAndCccd(phone, cccd);
        if (matchingCustomer.isPresent()) {
            return matchingCustomer.get();
        }

        if (hasCustomerWithPhoneOrCccd(phone, cccd)) {
            throw new AppException(ErrorCode.CUSTOMER_IDENTITY_MISMATCH);
        }
        return null;
    }

    private boolean hasCustomerWithPhoneOrCccd(String phone, String cccd) {
        Optional<Customer> customerByPhone = customerRepository.findByPhone(phone);
        Optional<Customer> customerByCccd = customerRepository.findByCccd(cccd);
        return customerByPhone.isPresent() || customerByCccd.isPresent();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerGetOneResponse updateCustomer(String id, iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerUpdateRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với mã: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            customer.setFullName(request.getName().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String phone = request.getPhone().trim();
            Optional<Customer> customerByPhone = customerRepository.findByPhone(phone);
            if (customerByPhone.isPresent()
                    && !customerByPhone.get().getId().equals(customer.getId())) {
                throw new AppException(ErrorCode.PHONE_EXISTED);
            }
            customer.setPhone(phone);
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            customer.setEmail(request.getEmail().trim());
        }
        if (request.getIdentityNumber() != null && !request.getIdentityNumber().isBlank()) {
            String cccd = request.getIdentityNumber().trim();
            Optional<Customer> customerByCccd = customerRepository.findByCccd(cccd);
            if (customerByCccd.isPresent()
                    && !customerByCccd.get().getId().equals(customer.getId())) {
                throw new AppException(ErrorCode.CCCD_EXISTED);
            }
            customer.setCccd(cccd);
        }

        Customer saved = customerRepository.save(customer);

        java.math.BigDecimal spent = saved.getTotalSpent() != null
                ? java.math.BigDecimal.valueOf(saved.getTotalSpent())
                : java.math.BigDecimal.ZERO;
        java.math.BigDecimal roomAmount = saved.getBookings() == null
                ? java.math.BigDecimal.ZERO
                : saved.getBookings().stream()
                .filter(booking -> booking.getOrder() != null && booking.getOrder().getRoomTotalAmount() != null)
                .map(booking -> booking.getOrder().getRoomTotalAmount())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerGetOneResponse.builder()
                .id(saved.getId())
                .fullName(saved.getFullName())
                .phone(saved.getPhone())
                .email(saved.getEmail())
                .cccd(saved.getCccd())
                .loyaltyTier(saved.getLoyaltyTier())
                .totalSpent(spent)
                .totalRoomAmount(roomAmount)
                .totalBookings(saved.getTotalBookings())
                .cancelledBookings(saved.getBookings() == null
                        ? 0
                        : (int) saved.getBookings().stream()
                        .filter(booking -> booking.getBookingStatus() != null
                                && booking.getBookingStatus().isCancelled())
                        .count())
                .build();
    }
}
