package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.config.SecurityUtils;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.CustomerRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.RoleRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ChangePasswordRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UpdateProfileRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserRegisterRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.EmployeeCreateResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.ProfileResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.UserService;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.shared.CloudinaryService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {
    EmployeeRepository employeeRepository;
    CustomerRepository customerRepository;
    AccountRepository accountRepository;
    RoleRepository roleRepository;
    HotelRepository hotelRepository;
    PasswordEncoder passwordEncoder;
    CloudinaryService cloudinaryService;

    private Account getCurrentAccount() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new RuntimeException("Bạn chưa đăng nhập");
        }
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản hiện tại"));
    }

    private ProfileResponse toProfileResponse(Account account) {
        if (account.getCustomer() != null) {
            Customer c = account.getCustomer();
            return ProfileResponse.builder()
                    .userId(c.getId())
                    .accountId(account.getId())
                    .fullName(c.getFullName())
                    .email(account.getEmail())
                    .phone(c.getPhone())
                    .cccd(c.getCccd())
                    .dateOfBirth(c.getDateOfBirth())
                    .avatarUrl(c.getAvatarUrl())
                    .position("Khách hàng")
                    .build();
        } else if (account.getEmployee() != null) {
            Employee e = account.getEmployee();
            return ProfileResponse.builder()
                    .userId(e.getId())
                    .accountId(account.getId())
                    .fullName(e.getFullName())
                    .email(account.getEmail())
                    .phone(e.getPhone())
                    .cccd(e.getCccd())
                    .dateOfBirth(e.getDateOfBirth())
                    .avatarUrl(e.getAvatarUrl())
                    .position(e.getPosition())
                    .build();
        } else {
            throw new RuntimeException("Tài khoản hiện tại chưa có hồ sơ người dùng");
        }
    }

    @Override
    @Transactional
    public ProfileResponse getMyProfile() {
        return toProfileResponse(getCurrentAccount());
    }

    @Override
    @Transactional
    public ProfileResponse updateMyProfile(UpdateProfileRequest request) {
        Account account = getCurrentAccount();

        if (account.getCustomer() != null) {
            Customer customer = account.getCustomer();

            // Kiểm tra trùng SĐT với khách hàng khác
            if (customer.getPhone() != null && !customer.getPhone().equals(request.getPhone())
                    && customerRepository.existsByPhone(request.getPhone())) {
                throw new RuntimeException("Số điện thoại này đã được sử dụng bởi tài khoản khác");
            }

            // Kiểm tra trùng CCCD với khách hàng khác
            if (request.getCccd() != null && !request.getCccd().isBlank()
                    && (customer.getCccd() == null || !customer.getCccd().equals(request.getCccd()))
                    && customerRepository.existsByCccd(request.getCccd())) {
                throw new RuntimeException("Số CCCD/CMND này đã được sử dụng bởi tài khoản khác");
            }

            customer.setFullName(request.getFullName());
            customer.setPhone(request.getPhone());
            customer.setCccd(request.getCccd());
            customer.setDateOfBirth(request.getDateOfBirth());
            customerRepository.save(customer);
            return toProfileResponse(account);
        } else if (account.getEmployee() != null) {
            Employee employee = account.getEmployee();
            employee.setFullName(request.getFullName());
            employee.setPhone(request.getPhone());
            employee.setCccd(request.getCccd());
            employee.setDateOfBirth(request.getDateOfBirth());
            employeeRepository.save(employee);
            return toProfileResponse(account);
        } else {
            throw new RuntimeException("Tài khoản hiện tại chưa có hồ sơ người dùng");
        }
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Xác nhận mật khẩu mới không khớp");
        }
        Account account = getCurrentAccount();
        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
    }

    @Transactional
    @Override
    public EmployeeCreateResponse createStaffAndAccount(UserRegisterRequest dto, MultipartFile avatarFile, Account currentAccount) {
        Set<String> creatorRoles = currentAccount.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        boolean isManager = creatorRoles.contains("ROLE_MANAGER");
        boolean isAdmin = creatorRoles.contains("ROLE_ADMIN");

        if (isManager && "Quản lý".equalsIgnoreCase(dto.getPosition())) {
            throw new RuntimeException("Quyền hạn bị từ chối: Quản lý chi nhánh chỉ được phép tạo tài khoản Nhân viên cấp dưới!");
        }

        if (isManager) {
            Long currentHotelId = currentAccount.getHotelId();
            if (currentHotelId != null) {
                dto.setHotelId(currentHotelId);
            } else {
                throw new RuntimeException("Lỗi hệ thống: Tài khoản Quản lý hiện tại chưa được cấu hình chi nhánh làm việc!");
            }
        } else if (isAdmin) {
            if (dto.getHotelId() == null) {
                throw new RuntimeException("Yêu cầu nhập liệu: Vui lòng lựa chọn chi nhánh khách sạn trực thuộc cho nhân sự mới!");
            }
        } else {
            throw new RuntimeException("Quyền hạn bị từ chối: Bạn không có đặc quyền thực hiện hành động này!");
        }

        // Kiểm tra trùng lặp Email
        if (accountRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Lỗi: Email này đã được đăng ký tài khoản trong hệ thống!");
        }

        // Upload avatar lên Cloudinary
        String uploadedUrl = "";
        if (avatarFile != null && !avatarFile.isEmpty()) {
            uploadedUrl = cloudinaryService.uploadImage(avatarFile, "avatars");
        }

        // Tìm chi nhánh khách sạn
        Hotel hotel = hotelRepository.findById(dto.getHotelId())
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy khách sạn có ID: " + dto.getHotelId()));

        // Phân loại Role
        String targetRoleName = "Quản lý".equalsIgnoreCase(dto.getPosition()) ? "ROLE_MANAGER" : "ROLE_EMPLOYEE";
        Role assignedRole = roleRepository.findByName(targetRoleName)
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Không tìm thấy vai trò " + targetRoleName + " dưới DB!"));

        // Tạo Account trước
        Account newAccount = Account.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode("1111"))
                .roles(Set.of(assignedRole))
                .build();

        // Tạo Employee
        Employee newEmployee = Employee.builder()
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .position(dto.getPosition())
                .avatarUrl(uploadedUrl)
                .hotel(hotel)
                .account(newAccount)
                .build();

        // Lưu Employee (Cascade lưu luôn Account)
        Employee savedEmployee = employeeRepository.save(newEmployee);

        return EmployeeCreateResponse.builder()
                .id(savedEmployee.getId())
                .fullName(savedEmployee.getFullName())
                .email(savedEmployee.getAccount().getEmail())
                .phone(savedEmployee.getPhone())
                .address(savedEmployee.getAddress())
                .position(savedEmployee.getPosition())
                .hotelName(hotel.getName())
                .roles(savedEmployee.getAccount().getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .build();
    }
}
