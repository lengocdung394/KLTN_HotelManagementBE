package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.CustomerRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.RoleRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerUpdateRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ForgotPasswordRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.SuperAdminAccountUpdateRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.SuperAdminAccountResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.AuthService;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.SuperAdminAccountService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SuperAdminAccountServiceImpl implements SuperAdminAccountService {

    private static final Set<String> STAFF_ROLES = Set.of(
            "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE");
    AccountRepository accountRepository;
    CustomerRepository customerRepository;
    RoleRepository roleRepository;
    AuthService authService;
    AccountSocketEmitter accountSocketEmitter;

    @Override
    public List<SuperAdminAccountResponse> getAccounts(String requestedType) {
        String type = requestedType == null || requestedType.isBlank()
                ? "ALL"
                : requestedType.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ALL", "STAFF", "CUSTOMERS", "REGISTERED_CUSTOMERS", "WALK_IN_CUSTOMERS").contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại danh sách tài khoản không hợp lệ.");
        }

        List<SuperAdminAccountResponse> accounts = accountRepository.findAllWithProfiles().stream()
                .map(this::toResponse)
                .filter(response -> !"WALK_IN_CUSTOMER".equals(response.getAccountType())
                        || response.getAccountId() != null)
                .filter(response -> matchesType(response.getAccountType(), type))
                .collect(Collectors.toList());


        if ("ALL".equals(type) || "CUSTOMERS".equals(type) || "WALK_IN_CUSTOMERS".equals(type)) {
            customerRepository.findAll().stream()
                    .filter(customer -> customer.getAccount() == null)
                    .map(this::toGuestResponse)
                    .filter(response -> matchesType(response.getAccountType(), type))
                    .forEach(accounts::add);
        }

        return accounts.stream()
                .sorted(Comparator.comparing(SuperAdminAccountResponse::getFullName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();


    }


    @Override
    public SuperAdminAccountResponse getAccountDetails(String id) {
        Account account = accountRepository.findByIdWithProfiles(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        return toResponse(account);
    }

    @Override
    public SuperAdminAccountResponse updateAccount(String id, SuperAdminAccountUpdateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu thông tin cập nhật.");
        }
        Account account = accountRepository.findByIdWithProfiles(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        String accountType = accountTypeOf(account);
        if ("SUPER_ADMIN".equals(accountType)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không thể cập nhật tài khoản Super Admin từ màn hình này.");
        }

        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (email.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không được để trống.");
            }
            if (accountRepository.existsByEmailIgnoreCaseAndIdNot(email, account.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng bởi tài khoản khác.");
            }
            account.setEmail(email);
            if (account.getCustomer() != null) {
                account.getCustomer().setEmail(email);
            }
        }
        requireNonBlankIfProvided(request.getFullName(), "Họ tên");
        requireNonBlankIfProvided(request.getPhone(), "Số điện thoại");

        Employee employee = account.getEmployee();
        Customer customer = account.getCustomer();
        if (employee != null) {
            if (request.getFullName() != null) employee.setFullName(request.getFullName().trim());
            if (request.getPhone() != null) employee.setPhone(request.getPhone().trim());
            if (request.getAddress() != null) employee.setAddress(request.getAddress().trim());
            if (request.getPosition() != null) employee.setPosition(request.getPosition().trim());
        } else if (customer != null) {
            if (request.getFullName() != null) customer.setFullName(request.getFullName().trim());
            if (request.getPhone() != null) customer.setPhone(request.getPhone().trim());
        }

        if (request.getRole() != null && !request.getRole().isBlank()) {
            updateStaffRole(account, request.getRole());
        }

        accountRepository.save(account);
        return toResponse(account);
    }

    @Override
    public boolean matchesType(String accountType, String filter) {
        return switch (filter) {
            case "ALL" -> true;
            case "STAFF" -> Set.of("ADMIN", "MANAGER", "EMPLOYEE").contains(accountType);
            case "CUSTOMERS" -> "CUSTOMER".equals(accountType) || "WALK_IN_CUSTOMER".equals(accountType);
            case "REGISTERED_CUSTOMERS" -> "CUSTOMER".equals(accountType);
            case "WALK_IN_CUSTOMERS" -> "WALK_IN_CUSTOMER".equals(accountType);
            default -> false;
        };
    }

    @Override
    public void updateStaffRole(Account account, String requestedRole) {
        if (account.getEmployee() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ tài khoản nhân sự mới được đổi vai trò.");
        }
        String roleName = requestedRole.trim().toUpperCase(Locale.ROOT);
        if (!STAFF_ROLES.contains(roleName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vai trò chỉ được là ROLE_ADMIN, ROLE_MANAGER hoặc ROLE_EMPLOYEE.");
        }
        Role newRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vai trò không tồn tại."));
        Set<Role> roles = (account.getRoles() == null ? Set.<Role>of() : account.getRoles()).stream()
                .filter(role -> !STAFF_ROLES.contains(role.getName()))
                .collect(Collectors.toSet());
        roles.add(newRole);
        account.setRoles(roles);
    }

    @Override
    public SuperAdminAccountResponse getCustomerDetails(String customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ khách hàng."));
        return customer.getAccount() == null ? toGuestResponse(customer) : toResponse(customer.getAccount());
    }

    @Override
    public SuperAdminAccountResponse updateCustomer(String customerId, CustomerUpdateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu thông tin cập nhật.");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ khách hàng."));

        if (request.getName() != null) customer.setFullName(request.getName().trim());
        if (request.getPhone() != null) customer.setPhone(request.getPhone().trim());
        if (request.getIdentityNumber() != null) customer.setCccd(request.getIdentityNumber().trim());
        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (customer.getAccount() != null
                    && accountRepository.existsByEmailIgnoreCaseAndIdNot(email, customer.getAccount().getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng bởi tài khoản khác.");
            }
            if (customer.getAccount() != null && email.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email tài khoản không được để trống.");
            }
            if (!email.isBlank() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không đúng định dạng.");
            }
            customer.setEmail(email);
            if (customer.getAccount() != null) customer.getAccount().setEmail(email);
        }

        Customer saved = customerRepository.save(customer);
        return saved.getAccount() == null ? toGuestResponse(saved) : toResponse(saved.getAccount());
    }

    @Override
    public void requestPasswordReset(String accountId) {
        Account account = accountRepository.findByIdWithProfiles(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        if ("WALK_IN_CUSTOMER".equals(accountTypeOf(account))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khách hàng vãng lai chưa có tài khoản web để đặt lại mật khẩu.");
        }
        if (account.getEmail() == null || account.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tài khoản chưa có email để nhận mã OTP đặt lại mật khẩu.");
        }
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(account.getEmail());
        authService.forgotPasswordRequest(request);
    }


    private SuperAdminAccountResponse toResponse(Account account) {
        Employee employee = account.getEmployee();
        Customer customer = account.getCustomer();
        Set<String> roles = account.getRoles() == null ? Set.of()
                : account.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        String accountType = accountTypeOf(account);

        return new SuperAdminAccountResponse(
                account.getId(),
                employee != null ? employee.getId() : customer != null ? customer.getId() : account.getId(),
                accountType,
                account.getEmail(),
                employee != null ? employee.getFullName() : customer != null ? customer.getFullName() : null,
                employee != null ? employee.getPhone() : customer != null ? customer.getPhone() : null,
                employee != null ? employee.getCccd() : customer != null ? customer.getCccd() : null,
                employee != null ? employee.getAddress() : null,
                employee != null ? employee.getPosition() : null,
                employee != null ? employee.getAvatarUrl() : null,
                employee != null && employee.getHotel() != null ? employee.getHotel().getId() : null,
                employee != null && employee.getHotel() != null ? employee.getHotel().getName() : null,
                customer == null ? roles.contains("ROLE_CUSTOMER") : customer.isRegistered(),
                customer == null || customer.getLoyaltyTier() == null ? null : customer.getLoyaltyTier().name(),
                roles
        );
    }

    private SuperAdminAccountResponse toGuestResponse(Customer customer) {
        return new SuperAdminAccountResponse(
                null,
                customer.getId(),
                "WALK_IN_CUSTOMER",
                customer.getEmail(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getCccd(),
                null,
                null,
                null,
                null,
                null,
                false,
                customer.getLoyaltyTier() == null ? null : customer.getLoyaltyTier().name(),
                Set.of()
        );
    }

    private String accountTypeOf(Account account) {
        Set<String> roles = account.getRoles() == null ? Set.of()
                : account.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        if (roles.contains("ROLE_SUPER_ADMIN")) return "SUPER_ADMIN";
        if (account.getEmployee() != null || roles.contains("ROLE_ADMIN")
                || roles.contains("ROLE_MANAGER") || roles.contains("ROLE_EMPLOYEE")) {
            if (roles.contains("ROLE_ADMIN")) return "ADMIN";
            if (roles.contains("ROLE_MANAGER")) return "MANAGER";
            return "EMPLOYEE";
        }
        if (account.getCustomer() != null) {
            return account.getCustomer().isRegistered() ? "CUSTOMER" : "WALK_IN_CUSTOMER";
        }
        if (roles.contains("ROLE_CUSTOMER")) return "CUSTOMER";
        return "OTHER";
    }

    private void requireNonBlankIfProvided(String value, String fieldName) {
        if (value != null && value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " không được để trống.");
        }
    }
}
