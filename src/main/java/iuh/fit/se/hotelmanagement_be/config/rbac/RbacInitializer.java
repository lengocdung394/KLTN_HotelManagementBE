package iuh.fit.se.hotelmanagement_be.config.rbac;


import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Permission;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.User;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.PermissionRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.RoleRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.UserRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Building;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Floor;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Province;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BuildingRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.FloorRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.ProvinceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Order(1)
public class RbacInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RbacConfig rbacConfig;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final HotelRepository hotelRepository;
    private final FloorRepository floorRepository;
    private final ProvinceRepository  provinceRepository;
    @Override
    @Transactional
    public void run(String... args) {

        Map<String, Permission> permissionMap = new HashMap<>();

        // 1. Tạo các Permission nếu chưa có
        for (String p : rbacConfig.getPermissions()) {
            Permission permission = permissionRepository.findByName(p)
                    .orElseGet(() -> permissionRepository.save(
                            Permission.builder().name(p).build()
                    ));
            permissionMap.put(p, permission);
        }

        // 2. Tạo Role + gán Permission
        for (String roleName : rbacConfig.getRoles().keySet()) {

            Role role = roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(
                            Role.builder()
                                    .name(roleName)
                                    .permissions(new HashSet<>())
                                    .build()
                    ));

            // Gán permission theo file config
            for (String p : rbacConfig.getRoles().get(roleName)) {
                role.getPermissions().add(permissionMap.get(p));
            }

            // Lưu lại role
            roleRepository.save(role);
        }

        System.out.println("RBAC initialization completed.");

        Hotel targetHotel;

        if (!hotelRepository.existsByName("Sài Gòn Sky Hotel & Residence")) {
            // 1. Tạo Tỉnh/Thành phố
            Province province = provinceRepository.save(
                    Province.builder().name("TP. Hồ Chí Minh").build()
            );

            // 2. Tạo Khách sạn
            targetHotel = hotelRepository.save(
                    Hotel.builder()
                            .name("Sài Gòn Sky Hotel & Residence")
                            .address("123 Lê Lợi, Quận 1, TP. HCM")
                            .phone("0283999999")
                            .province(province)
                            .build()
            );

            // 3. Tạo Tòa nhà (Building) Mercury
            Building building = buildingRepository.save(
                    Building.builder().name("Mercury").hotel(targetHotel).build()
            );

            // 4. Tạo các Tầng (Floor 1 và Floor 2)
            floorRepository.save(Floor.builder().floorNumber(1).building(building).build());
            floorRepository.save(Floor.builder().floorNumber(2).building(building).build());

            System.out.println(">>> [STARTUP] Đã khởi tạo dữ liệu Khách sạn, Tòa nhà và Tầng mẫu.");
        } else {
            // Nếu đã chạy lần 2, lấy khách sạn cũ ra để gán cho Admin tổng nếu cần
            targetHotel = hotelRepository.findAll().get(0);
        }

        String adminEmail = "admin@senviet.vn";

        if (!accountRepository.existsByEmail(adminEmail)) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Lỗi cấu hình: File JSON thiếu ROLE_ADMIN"));

            // Tạo thông tin cá nhân Admin
            User adminUser = User.builder()
                    .fullName("Admin Tổng Toàn Hệ Thống")
                    .phone("0901234567")
                    .position("Admin tổng")
                    .hotel(targetHotel) // Gắn Admin tối cao thuộc biên chế khách sạn đầu tiên
                    .build();

            // Tạo tài khoản bảo mật
            Account adminAccount = Account.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode("admin123")) // pass login: admin123
                    .roles(Set.of(adminRole))
                    .build();

            adminAccount.setUser(adminUser);
            adminUser.setAccount(adminAccount);

            userRepository.save(adminUser);
            System.out.println(">>> [STARTUP] ĐÃ KHỞI TẠO THÀNH CÔNG TÀI KHOẢN ADMIN TỔI CAO: " + adminEmail);
        }
    }
}

