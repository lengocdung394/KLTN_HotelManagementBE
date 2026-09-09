package iuh.fit.se.hotelmanagement_be.config.rbac;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Permission;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.PermissionRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.RoleRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.*;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.*;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Order(1)
public class RbacInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RbacConfig rbacConfig;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeRepository employeeRepository;
    private final BuildingRepository buildingRepository;
    private final HotelRepository hotelRepository;
    private final FloorRepository floorRepository;
    private final ProvinceRepository provinceRepository;
    private final BranchRoomPolicyRepository branchRoomPolicyRepository;

    @Override
    @Transactional
    public void run(String... args) {

        // ==========================================
        // 1. KHỞI TẠO PERMISSIONS & ROLES (RBAC)
        // ==========================================
        Map<String, Permission> permissionMap = new HashMap<>();

        for (String p : rbacConfig.getPermissions()) {
            Permission permission = permissionRepository.findByName(p)
                    .orElseGet(() -> permissionRepository.save(
                            Permission.builder().name(p).build()
                    ));
            permissionMap.put(p, permission);
        }

        for (String roleName : rbacConfig.getRoles().keySet()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(
                            Role.builder()
                                    .name(roleName)
                                    .permissions(new HashSet<>())
                                    .build()
                    ));

            for (String p : rbacConfig.getRoles().get(roleName)) {
                role.getPermissions().add(permissionMap.get(p));
            }
            roleRepository.save(role);
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("Lỗi cấu hình: File JSON thiếu ROLE_ADMIN"));

        System.out.println(">>> [STARTUP] Khởi tạo hệ thống Permission & Role hoàn tất.");

        if (!hotelRepository.existsByName("Sài Gòn Sky Hotel & Residence")) {
            Province provinceHcm = provinceRepository.save(Province.builder().name("TP. Hồ Chí Minh").build());

            Hotel hotel1 = hotelRepository.save(
                    Hotel.builder()
                            .name("Sài Gòn Sky Hotel & Residence")
                            .address("123 Lê Lợi, Quận 1, TP. HCM")
                            .phone("0283999999")
                            .province(provinceHcm)
                            .build()
            );

            // Tòa nhà & Tầng mẫu cho Chi nhánh 1
            Building b1 = buildingRepository.save(Building.builder().name("Tòa A - Sài Gòn").hotel(hotel1).build());
            floorRepository.save(Floor.builder().floorNumber(1).building(b1).build());
            floorRepository.save(Floor.builder().floorNumber(2).building(b1).build());

            // 👉 KHỞI TẠO CHÍNH SÁCH PHÒNG CHO CHI NHÁNH 1 (Sài Gòn)
            branchRoomPolicyRepository.saveAll(List.of(
                    BranchRoomPolicy.builder()
                            .hotel(hotel1)
                            .roomType(RoomType.STANDARD) // Đã sửa từ SUITE thành SINGLE
                            .standardAdults(1)
                            .maxAdults(1)
                            .maxInfants(1)
                            .maxChildren(1)
                            .extraAdultFee(150000.0)
                            .extraChildFee(80000.0)
                            .build(),
                    BranchRoomPolicy.builder()
                            .hotel(hotel1)
                            .roomType(RoomType.DELUXE)
                            .standardAdults(2)
                            .maxAdults(2)
                            .maxChildren(2)
                            .maxInfants(1)
                            .extraAdultFee(200000.0)
                            .extraChildFee(100000.0)
                            .build(),
                    BranchRoomPolicy.builder()
                            .hotel(hotel1)
                            .roomType(RoomType.SUITE)
                            .standardAdults(3) // Chỉnh lại số lượng chuẩn cho hợp lý (vd: 3)
                            .maxAdults(3)
                            .maxChildren(2)
                            .maxInfants(1)
                            .extraAdultFee(300000.0)
                            .extraChildFee(150000.0)
                            .build()
            ));
            // Admin Chi nhánh 1
            createBranchAdmin(
                    "admin.saigon@senviet.vn",
                    "Quản Lý Sài Gòn",
                    "0901111111",
                    "Admin Chi nhánh Sài Gòn",
                    hotel1,
                    adminRole
            );
            System.out.println(">>> [STARTUP] Đã tạo Chi nhánh 1: Sài Gòn Sky Hotel & Policy kèm theo.");
        }

        // ==========================================
        // 3. KHỞI TẠO CHI NHÁNH 2: HÀ NỘI
        // ==========================================
        if (!hotelRepository.existsByName("Hà Nội Grand Hotel")) {
            Province provinceHanoi = provinceRepository.save(Province.builder().name("TP. Hà Nội").build());

            Hotel hotel2 = hotelRepository.save(
                    Hotel.builder()
                            .name("Hà Nội Grand Hotel")
                            .address("45 Tràng Tiền, Hoàn Kiếm, Hà Nội")
                            .phone("0243888888")
                            .province(provinceHanoi)
                            .build()
            );

            // Tòa nhà & Tầng mẫu cho Chi nhánh 2
            Building b2 = buildingRepository.save(Building.builder().name("Tòa Hoàn Kiếm - Hà Nội").hotel(hotel2).build());
            floorRepository.save(Floor.builder().floorNumber(1).building(b2).build());
            floorRepository.save(Floor.builder().floorNumber(2).building(b2).build());

            // 👉 KHỞI TẠO CHÍNH SÁCH PHÒNG CHO CHI NHÁNH 2 (Hà Nội - Giá có thể nhỉnh hơn chút)
            branchRoomPolicyRepository.saveAll(List.of(
                    BranchRoomPolicy.builder()
                            .hotel(hotel2)
                            .roomType(RoomType.DELUXE)
                            .standardAdults(1)
                            .maxAdults(1)
                            .maxChildren(1)
                            .maxInfants(1)
                            .extraAdultFee(180000.0)
                            .extraChildFee(90000.0)
                            .build(),
                    BranchRoomPolicy.builder()
                            .hotel(hotel2)
                            .roomType(RoomType.STANDARD)
                            .standardAdults(2)
                            .maxAdults(2)
                            .maxChildren(2)
                            .maxInfants(1)
                            .extraAdultFee(250000.0)
                            .extraChildFee(120000.0)
                            .build(),
                    BranchRoomPolicy.builder()
                            .hotel(hotel2)
                            .roomType(RoomType.SUITE)
                            .standardAdults(4)
                            .maxAdults(3)
                            .maxChildren(2)
                            .maxInfants(1)
                            .extraAdultFee(350000.0)
                            .extraChildFee(180000.0)
                            .build()
            ));

            // Admin Chi nhánh 2
            createBranchAdmin(
                    "admin.hanoi@senviet.vn",
                    "Quản Lý Hà Nội",
                    "0902222222",
                    "Admin Chi nhánh Hà Nội",
                    hotel2,
                    adminRole
            );
            System.out.println(">>> [STARTUP] Đã tạo Chi nhánh 2: Hà Nội Grand Hotel & Policy kèm theo.");
        }
        // ==========================================
        // 4. KHỞI TẠO ADMIN TỔNG (SUPER ADMIN)
        // ==========================================
        String superAdminEmail = "admin@senviet.vn";
        if (!accountRepository.existsByEmail(superAdminEmail)) {
            //  1. Tạo Account trước
            Account superAdminAccount = Account.builder()
                    .email(superAdminEmail)
                    .password(passwordEncoder.encode("admin123"))
                    .roles(Set.of(adminRole))
                    .build();

            //  2. Tạo Employee gắn Account vào
            Employee superAdminEmployee = Employee.builder()
                    .fullName("Admin Tổng Toàn Hệ Thống")
                    .phone("0901234567")
                    .position("Super Admin")
                    .hotel(null) // null đại diện cho việc quản lý toàn bộ hệ thống
                    .account(superAdminAccount)
                    .build();

            //  3. Lưu Employee
            employeeRepository.save(superAdminEmployee);
            System.out.println(">>> [STARTUP] Đã tạo Tài khoản Admin Tổng: " + superAdminEmail);
        }
    }

    /**
     * Helper method tạo tài khoản Admin cho chi nhánh
     */
    private void createBranchAdmin(String email, String fullName, String phone, String position, Hotel hotel, Role role) {
        if (!accountRepository.existsByEmail(email)) {
            Account account = Account.builder()
                    .email(email)
                    .password(passwordEncoder.encode("admin123"))
                    .roles(Set.of(role))
                    .build();

            Employee employee = Employee.builder()
                    .fullName(fullName)
                    .phone(phone)
                    .position(position)
                    .hotel(hotel)
                    .account(account)
                    .build();

            employeeRepository.save(employee);
        }
    }
}