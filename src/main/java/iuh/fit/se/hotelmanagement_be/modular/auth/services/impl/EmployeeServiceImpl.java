package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.CustomerRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.RoleRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserRegisterRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.EmployeeProfileUpdateRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.EmployeeCreateResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.EmployeeResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.EmployeeService;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import iuh.fit.se.hotelmanagement_be.modular.notification.services.NotificationService;
import iuh.fit.se.hotelmanagement_be.shared.CloudinaryService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmployeeServiceImpl implements EmployeeService {
    EmployeeRepository employeeRepository;
    AccountRepository accountRepository;
    CustomerRepository customerRepository;
    RoleRepository roleRepository;
    HotelRepository hotelRepository;
    PasswordEncoder passwordEncoder;
    CloudinaryService cloudinaryService;
    NotificationService notificationService;
    CustomerSocketEmitter customerSocketEmitter;

    // Sử dụng khi quản lí tạo tài khoản cho nhân viên (kèm theo thông tin cá nhân)
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

//        if (isManager) {
//            Long currentHotelId = currentAccount.getHotelId();
//            if (currentHotelId != null) {
//                dto.setHotelId(currentHotelId);
//            } else {
//                throw new RuntimeException("Lỗi hệ thống: Tài khoản Quản lý hiện tại chưa được cấu hình chi nhánh làm việc!");
//            }
//        } else if (isAdmin) {
//            if (dto.getHotelId() == null) {
//                throw new RuntimeException("Yêu cầu nhập liệu: Vui lòng lựa chọn chi nhánh khách sạn trực thuộc cho nhân sự mới!");
//            }
//        } else {
//            throw new RuntimeException("Quyền hạn bị từ chối: Bạn không có đặc quyền thực hiện hành động này!");
//        }

        String email = dto.getEmail() == null ? "" : dto.getEmail().trim();
        String phone = dto.getPhone() == null ? "" : dto.getPhone().trim();
        String cccd = dto.getCccd() == null ? "" : dto.getCccd().trim();
        dto.setEmail(email);
        dto.setPhone(phone);
        dto.setCccd(cccd);

        if (accountRepository.existsByEmailIgnoreCase(email)
                || customerRepository.existsByEmailIgnoreCase(email)) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }
        if (employeeRepository.existsByPhone(phone) || customerRepository.existsByPhone(phone)) {
            throw new AppException(ErrorCode.PHONE_EXISTED);
        }
        if (employeeRepository.existsByCccd(cccd) || customerRepository.existsByCccd(cccd)) {
            throw new AppException(ErrorCode.CCCD_EXISTED);
        }

        String uploadedUrl = dto.getAvatarUrl() == null ? "" : dto.getAvatarUrl().trim();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            uploadedUrl = cloudinaryService.uploadImage(avatarFile, "avatars");
        }

        // Tìm chi nhánh khách sạn
        Hotel hotel = hotelRepository.findById(dto.getHotelId())
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy khách sạn có ID: " + dto.getHotelId()));

        // Phân loại Role
        String targetRoleName = "Quản lý".equalsIgnoreCase(dto.getPosition()) ? "ROLE_MANAGER" : "ROLE_EMPLOYEE";
        Role assignedRole = roleRepository.findByCode(targetRoleName)
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
                .cccd(dto.getCccd())
                .address(dto.getAddress())
                .position(dto.getPosition())
                .avatarUrl(uploadedUrl)
                .hotel(hotel)
                .account(newAccount)
                .build();

        // Lưu Employee (Cascade lưu luôn Account)
        Employee savedEmployee = employeeRepository.save(newEmployee);
        notificationService.createAndPublish(
                hotel.getId(),
                NotificationType.EMPLOYEE_CREATED,
                "Nhân viên mới",
                "Nhân viên " + savedEmployee.getFullName() + " vừa được thêm vào chi nhánh.",
                "EMPLOYEE",
                savedEmployee.getId());
        customerSocketEmitter.emitEmployeeCreated(hotel.getId(), toEmployeeResponse(savedEmployee));
        return EmployeeCreateResponse.builder()
                .id(savedEmployee.getId())
                .fullName(savedEmployee.getFullName())
                .email(savedEmployee.getAccount().getEmail())
                .phone(savedEmployee.getPhone())
                .cccd(savedEmployee.getCccd())
                .address(savedEmployee.getAddress())
                .position(savedEmployee.getPosition())
                .hotelName(hotel.getName())
                .roles(savedEmployee.getAccount().getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .build();
    }

    @Override
    public List<EmployeeResponse> getEmployeesByHotelId(Long hotelId) {
        List<Employee> list = (hotelId == null)
                ? employeeRepository.findAll()
                : employeeRepository.findByHotelId(hotelId);
        return list.stream()
                .map(this::toEmployeeResponse)
                .collect(Collectors.toList());
    }

    @Override
    public EmployeeResponse getEmployeeById(String id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên có ID: " + id));
        return toEmployeeResponse(emp);
    }

    @Transactional
    @Override
    public EmployeeResponse updateEmployee(String id, UserRegisterRequest dto, MultipartFile avatarFile, Account currentAccount) {
        Set<String> updaterRoles = currentAccount.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());
        if (!updaterRoles.contains("ROLE_MANAGER") && !updaterRoles.contains("ROLE_ADMIN")) {
            throw new RuntimeException("Quyền hạn bị từ chối: Chỉ Quản lý chi nhánh hoặc Admin chi nhánh được cập nhật nhân viên!");
        }

        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên có ID: " + id));
        Long currentHotelId = currentAccount.getHotelId();
        if (currentHotelId == null || emp.getHotel() == null || !currentHotelId.equals(emp.getHotel().getId())) {
            throw new RuntimeException("Quyền hạn bị từ chối: Bạn chỉ được cập nhật nhân viên thuộc chi nhánh của mình!");
        }
        if (emp.getAccount() == null || emp.getAccount().getRoles().stream()
                .noneMatch(role -> "ROLE_EMPLOYEE".equals(role.getCode()))) {
            throw new RuntimeException("Chỉ có thể cập nhật tài khoản nhân viên cấp dưới.");
        }

        // Kiểm tra trùng SĐT nếu thay đổi
        if (dto.getPhone() != null && !dto.getPhone().isBlank() && !dto.getPhone().equals(emp.getPhone())) {
            if (employeeRepository.existsByPhoneAndIdNot(dto.getPhone(), id)) {
                throw new RuntimeException("Số điện thoại này đã được sử dụng bởi nhân viên khác!");
            }
            emp.setPhone(dto.getPhone());
        }

        // Kiểm tra trùng CCCD nếu thay đổi
        if (dto.getCccd() != null && !dto.getCccd().isBlank() && !dto.getCccd().equals(emp.getCccd())) {
            if (employeeRepository.existsByCccdAndIdNot(dto.getCccd(), id)) {
                throw new RuntimeException("Số CCCD này đã được sử dụng bởi nhân viên khác!");
            }
            emp.setCccd(dto.getCccd());
        }

        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
            emp.setFullName(dto.getFullName());
        }
        if (dto.getAddress() != null) {
            emp.setAddress(dto.getAddress());
        }
        if (dto.getPosition() != null && !dto.getPosition().isBlank()) {
            emp.setPosition(dto.getPosition());
        }
        if (dto.getEmail() != null) {
            String email = dto.getEmail().trim();
            if (email.isBlank()) {
                throw new RuntimeException("Email không được để trống!");
            }
            if (!email.equalsIgnoreCase(emp.getAccount().getEmail())
                    && accountRepository.existsByEmailIgnoreCaseAndIdNot(email, emp.getAccount().getId())) {
                throw new RuntimeException("Email này đã được sử dụng bởi tài khoản khác!");
            }
            emp.getAccount().setEmail(email);
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            if (dto.getPassword().length() < 8) {
                throw new RuntimeException("Mật khẩu mới phải có ít nhất 8 ký tự!");
            }
            emp.getAccount().setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        // Upload avatar nếu có file mới
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String uploadedUrl = cloudinaryService.uploadImage(avatarFile, "avatars");
            emp.setAvatarUrl(uploadedUrl);
        } else if (dto.getAvatarUrl() != null && !dto.getAvatarUrl().isBlank()) {
            emp.setAvatarUrl(dto.getAvatarUrl().trim());
        }

        Employee saved = employeeRepository.save(emp);
        return toEmployeeResponse(saved);
    }

    @Transactional
    @Override
    public EmployeeResponse updateMyProfile(EmployeeProfileUpdateRequest request, Account currentAccount) {
        Account account = accountRepository.findById(currentAccount.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản."));
        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không chính xác.");
        }

        Employee employee = account.getEmployee();
        if (employee == null) {
            throw new RuntimeException("Tài khoản này không có hồ sơ nhân viên.");
        }

        String email = request.getEmail().trim();
        if (!email.equalsIgnoreCase(account.getEmail())
                && accountRepository.existsByEmailIgnoreCaseAndIdNot(email, account.getId())) {
            throw new RuntimeException("Email này đã được sử dụng bởi tài khoản khác.");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && !request.getPhone().equals(employee.getPhone())
                && employeeRepository.existsByPhoneAndIdNot(request.getPhone(), employee.getId())) {
            throw new RuntimeException("Số điện thoại này đã được sử dụng bởi nhân viên khác.");
        }
        if (request.getCccd() != null && !request.getCccd().isBlank()
                && !request.getCccd().equals(employee.getCccd())
                && employeeRepository.existsByCccdAndIdNot(request.getCccd(), employee.getId())) {
            throw new RuntimeException("Số CCCD này đã được sử dụng bởi nhân viên khác.");
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getNewPassword().length() < 8) {
                throw new RuntimeException("Mật khẩu mới phải có ít nhất 8 ký tự.");
            }
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new RuntimeException("Mật khẩu xác nhận không khớp.");
            }
            account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        } else if (request.getConfirmPassword() != null && !request.getConfirmPassword().isBlank()) {
            throw new RuntimeException("Vui lòng nhập mật khẩu mới.");
        }

        account.setEmail(email);
        employee.setFullName(request.getFullName().trim());
        employee.setPosition(request.getPosition());
        employee.setAddress(request.getAddress());
        employee.setPhone(request.getPhone());
        employee.setCccd(request.getCccd());
        Employee saved = employeeRepository.save(employee);

        return toEmployeeResponse(saved);
    }

    private EmployeeResponse toEmployeeResponse(Employee emp) {
        if (emp == null) return null;
        Set<String> roles = (emp.getAccount() != null && emp.getAccount().getRoles() != null)
                ? emp.getAccount().getRoles().stream().map(Role::getName).collect(Collectors.toSet())
                : Set.of();
        Set<String> roleCodes = (emp.getAccount() != null && emp.getAccount().getRoles() != null)
                ? emp.getAccount().getRoles().stream().map(Role::getCode).collect(Collectors.toSet())
                : Set.of();
        String email = (emp.getAccount() != null) ? emp.getAccount().getEmail() : null;
        Long hotelId = (emp.getHotel() != null) ? emp.getHotel().getId() : null;
        String hotelName = (emp.getHotel() != null) ? emp.getHotel().getName() : null;

        return EmployeeResponse.builder()
                .id(emp.getId())
                .email(email)
                .fullName(emp.getFullName())
                .phone(emp.getPhone())
                .cccd(emp.getCccd())
                .address(emp.getAddress())
                .position(emp.getPosition())
                .avatarUrl(emp.getAvatarUrl())
                .dateOfBirth(emp.getDateOfBirth())
                .hotelId(hotelId)
                .hotelName(hotelName)
                .roles(roles)
                .roleCodes(roleCodes)
                .build();
    }
}
