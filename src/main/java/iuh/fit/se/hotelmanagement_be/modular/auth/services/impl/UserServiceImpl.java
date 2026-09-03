package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.User;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.RoleRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.UserRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserRegisterRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.UserCreateResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.UserResponse;
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
    UserRepository userRepository;
    AccountRepository accountRepository;
    RoleRepository roleRepository;
    HotelRepository hotelRepository;
    PasswordEncoder passwordEncoder;
    CloudinaryService cloudinaryService;

    @Transactional
    @Override
    public UserCreateResponse createStaffAndAccount(UserRegisterRequest dto, MultipartFile avatarFile, Account currentAccount) {
        Set<String> creatorRoles = currentAccount.getRoles().stream()
                .map(role -> role.getName())
                .collect(java.util.stream.Collectors.toSet());


        boolean isManager = creatorRoles.contains("ROLE_MANAGER");
        boolean isAdmin = creatorRoles.contains("ROLE_ADMIN");


        if (isManager && "Quản lý".equalsIgnoreCase(dto.getPosition())) {
            throw new RuntimeException("Quyền hạn bị từ chối: Quản lý chi nhánh chỉ được phép tạo tài khoản Nhân viên cấp dưới!");
        }

        if (isManager) {
            // Nếu là Quản lý tạo, hệ thống tự động bốc mã khách sạn của ông Quản lý này gắn cho nhân viên mới
            if (currentAccount.getUser() != null && currentAccount.getUser().getHotel() != null) {
                dto.setHotelId(currentAccount.getUser().getHotel().getId());
            } else {
                throw new RuntimeException("Lỗi hệ thống: Tài khoản Quản lý hiện tại chưa được cấu hình chi nhánh làm việc!");
            }
        } else if (isAdmin) {
            // Nếu là Admin tổng tạo, bắt buộc form Frontend phải chọn và truyền lên mã khách sạn trực thuộc
            if (dto.getHotelId() == null) {
                throw new RuntimeException("Yêu cầu nhập liệu: Vui lòng lựa chọn chi nhánh khách sạn trực thuộc cho nhân sự mới!");
            }
        } else {
            // Tài khoản đột nhập không có cả 2 quyền trên
            throw new RuntimeException("Quyền hạn bị từ chối: Bạn không có đặc quyền thực hiện hành động này!");
        }

        // 3. Kiểm tra trùng lặp Email dưới bảng accounts
        if (accountRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Lỗi: Email này đã được đăng ký tài khoản trong hệ thống!");
        }

        // 4. Xử lý đẩy file ảnh lên gói thư mục 'avatars' của Cloudinary (Nếu có chọn ảnh)
        String uploadedUrl = "";
        if (avatarFile != null && !avatarFile.isEmpty()) {
            uploadedUrl = cloudinaryService.uploadImage(avatarFile, "avatars");
        }

        // 5. Tìm chi nhánh khách sạn trực thuộc dưới DB
        Hotel hotel = hotelRepository.findById(dto.getHotelId())
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy khách sạn có ID: " + dto.getHotelId()));

        // 6. Phân loại gán Role hệ thống bốc từ file JSON cấu hình của bạn
        String targetRoleName = "Quản lý".equalsIgnoreCase(dto.getPosition()) ? "ROLE_MANAGER" : "ROLE_EMPLOYEE";
        Role assignedRole = roleRepository.findByName(targetRoleName)
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Không tìm thấy vai trò " + targetRoleName + " dưới DB!"));

        // 7. Khởi tạo thực thể thông tin cá nhân (User)
        User newUser = User.builder()
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .cccd(dto.getCccd())
                .address(dto.getAddress())
                .position(dto.getPosition())
                .avatarUrl(uploadedUrl) // Lưu link URL bốc từ Cloudinary về
                .hotel(hotel)
                .build();

        // 8. Tự động cấp tài khoản đăng nhập (Account) với mật khẩu mặc định '1111'
        Account newAccount = Account.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode("1111")) // Mã hóa pass mặc định
                .roles(Set.of(assignedRole))
                .build();

        // Thiết lập mối quan hệ liên kết song phương 1-1
        newAccount.setUser(newUser);
        newUser.setAccount(newAccount);

        // Lưu xuống DB qua cơ chế Cascade
        User savedUser = userRepository.save(newUser);
        return UserCreateResponse.builder()
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getAccount().getEmail())
                .phone(savedUser.getPhone())
                .address(savedUser.getAddress())
                .cccd(savedUser.getCccd())
                .position(savedUser.getPosition())
                .hotelName(hotel.getName())
                .roles(savedUser.getAccount().getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .build();
    }

}
