package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.RoleRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserRegisterRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.EmployeeCreateResponse;
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
    AccountRepository accountRepository;
    RoleRepository roleRepository;
    HotelRepository hotelRepository;
    PasswordEncoder passwordEncoder;
    CloudinaryService cloudinaryService;

    @Transactional
    @Override
    public EmployeeCreateResponse createStaffAndAccount(UserRegisterRequest dto, MultipartFile avatarFile, Account currentAccount) {
        Set<String> creatorRoles = currentAccount.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        boolean isManager = creatorRoles.contains("ROLE_MANAGER");
        boolean isAdmin = creatorRoles.contains("ROLE_ADMIN");

        if (isManager && "Quản lý".equalsIgnoreCase(dto.getPosition())) {
            throw new RuntimeException("Quyen han bi tu choi"); // Hoặc RuntimeException("Quyền hạn bị từ chối: Quản lý chi nhánh chỉ được phép tạo tài khoản Nhân viên cấp dưới!");
        }

        if (isManager) {
            // 💡 SỬA LỖI 1: Lấy hotelId từ Employee hoặc hàm getHotelId() trong Account
            Long currentHotelId = currentAccount.getHotelId(); // Hàm helper đã định nghĩa ở Account entity
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
            throw new RuntimeException("Email da ton tai");
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

        // 💡 SỬA LỖI 2: Tạo Account trước
        Account newAccount = Account.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode("1111"))
                .roles(Set.of(assignedRole))
                .build();

        // 💡 SỬA LỖI 3: Tạo Employee thay vì User
        Employee newEmployee = Employee.builder()
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .position(dto.getPosition())
                .avatarUrl(uploadedUrl)
                .hotel(hotel)
                .account(newAccount) // Gán Account cho Employee (sở hữu khóa ngoại account_id)
                .build();

        // 💡 SỬA LỖI 4: Lưu Employee (sẽ tự động Cascade lưu luôn Account)
        Employee savedEmployee = employeeRepository.save(newEmployee);

        // Trả về DTO kết quả
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
