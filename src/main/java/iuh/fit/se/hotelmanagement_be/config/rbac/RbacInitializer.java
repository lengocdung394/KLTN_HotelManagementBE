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
import iuh.fit.se.hotelmanagement_be.modular.room.entities.BedType;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomTypeBed;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.BedTypeRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomTypeBedRepository;
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
    private final RoomTypeBedRepository roomTypeBedRepository;
    private final RoomRepository roomRepository;
    private final BedTypeRepository bedTypeRepository;

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

        // ==========================================
        // 2. KHỞI TẠO CHI NHÁNH 1: SÀI GÒN
        // ==========================================
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
                            .roomType(RoomType.STANDARD)
                            .standardAdults(1)
                            .maxAdults(2)
                            .maxChildren(2)
                            .maxInfants(1)
                            .extraAdultFee(250000.0)
                            .extraChildFee(120000.0)
                            .build(),
                    BranchRoomPolicy.builder()
                            .hotel(hotel1)
                            .roomType(RoomType.DELUXE)
                            .standardAdults(2)
                            .maxAdults(3)
                            .maxChildren(2)
                            .maxInfants(1)
                            .extraAdultFee(180000.0)
                            .extraChildFee(90000.0)
                            .build(),
                    BranchRoomPolicy.builder()
                            .hotel(hotel1)
                            .roomType(RoomType.SUITE)
                            .standardAdults(2)
                            .maxAdults(4)
                            .maxChildren(3)
                            .maxInfants(2)
                            .extraAdultFee(350000.0)
                            .extraChildFee(180000.0)
                            .build(),
                    BranchRoomPolicy.builder()
                            .hotel(hotel1)
                            .roomType(RoomType.FAMILY)
                            .standardAdults(4)
                            .maxAdults(6)
                            .maxChildren(4)
                            .maxInfants(2)
                            .extraAdultFee(280000.0)
                            .extraChildFee(140000.0)
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

            // 👉 KHỞI TẠO CHÍNH SÁCH PHÒNG CHO CHI NHÁNH 2 (Hà Nội)
            branchRoomPolicyRepository.saveAll(List.of(
                    BranchRoomPolicy.builder()
                            .hotel(hotel2)
                            .roomType(RoomType.STANDARD)
                            .standardAdults(1)
                            .maxAdults(2)
                            .maxChildren(2)
                            .maxInfants(1)
                            .extraAdultFee(250000.0)
                            .extraChildFee(120000.0)
                            .build(),
                    BranchRoomPolicy.builder()
                            .hotel(hotel2)
                            .roomType(RoomType.DELUXE)
                            .standardAdults(2)
                            .maxAdults(3)
                            .maxChildren(2)
                            .maxInfants(1)
                            .extraAdultFee(180000.0)
                            .extraChildFee(90000.0)
                            .build(),
                    BranchRoomPolicy.builder()
                            .hotel(hotel2)
                            .roomType(RoomType.SUITE)
                            .standardAdults(2)
                            .maxAdults(4)
                            .maxChildren(3)
                            .maxInfants(2)
                            .extraAdultFee(350000.0)
                            .extraChildFee(180000.0)
                            .build(),
                    BranchRoomPolicy.builder()
                            .hotel(hotel2)
                            .roomType(RoomType.FAMILY)
                            .standardAdults(4)
                            .maxAdults(6)
                            .maxChildren(4)
                            .maxInfants(2)
                            .extraAdultFee(280000.0)
                            .extraChildFee(140000.0)
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
            Account superAdminAccount = Account.builder()
                    .email(superAdminEmail)
                    .password(passwordEncoder.encode("admin123"))
                    .roles(Set.of(adminRole))
                    .build();

            Employee superAdminEmployee = Employee.builder()
                    .fullName("Admin Tổng Toàn Hệ Thống")
                    .phone("0901234567")
                    .position("Super Admin")
                    .hotel(null)
                    .account(superAdminAccount)
                    .build();

            employeeRepository.save(superAdminEmployee);
            System.out.println(">>> [STARTUP] Đã tạo Tài khoản Admin Tổng: " + superAdminEmail);
        }

        // ==========================================
        // 5. KHỞI TẠO DANH MỤC LOẠI GIƯỜNG & PHÂN BỔ GIƯỜNG
        // ==========================================
        if (bedTypeRepository.count() == 0) {
            bedTypeRepository.saveAll(List.of(
                    BedType.builder().name("Single Bed").description("Giường đơn tiêu chuẩn kích thước 1m2 x 2m").capacity(1).isExtraBed(false).build(),
                    BedType.builder().name("Queen Bed").description("Giường đôi vừa kích thước 1m6 x 2m").capacity(2).isExtraBed(false).build(),
                    BedType.builder().name("King Bed").description("Giường đôi lớn kích thước 1m8 x 2m").capacity(2).isExtraBed(false).build(),
                    BedType.builder().name("Super King Bed").description("Giường đôi siêu lớn kích thước 2m x 2m2").capacity(2).isExtraBed(false).build(),
                    BedType.builder().name("Sofa Bed").description("Giường sofa đa năng đặt tại phòng khách").capacity(2).isExtraBed(true).build(),
                    BedType.builder().name("Extra Bed").description("Giường phụ di động kê thêm khi có yêu cầu").capacity(1).isExtraBed(true).build()
            ));
            System.out.println(">>> [STARTUP] Đã khởi tạo danh mục các Loại giường.");
        }

        if (roomTypeBedRepository.count() == 0) {
            BedType queenBed = bedTypeRepository.findByName("Queen Bed");
            BedType kingBed = bedTypeRepository.findByName("King Bed");
            BedType superKingBed = bedTypeRepository.findByName("Super King Bed");
            BedType sofaBed = bedTypeRepository.findByName("Sofa Bed");

            if (queenBed != null && kingBed != null && superKingBed != null && sofaBed != null) {
                roomTypeBedRepository.saveAll(List.of(
                        RoomTypeBed.builder().roomType(RoomType.STANDARD).bedType(queenBed).quantity(1).build(),
                        RoomTypeBed.builder().roomType(RoomType.DELUXE).bedType(kingBed).quantity(1).build(),
                        RoomTypeBed.builder().roomType(RoomType.SUITE).bedType(superKingBed).quantity(1).build(),
                        RoomTypeBed.builder().roomType(RoomType.SUITE).bedType(sofaBed).quantity(1).build(),
                        RoomTypeBed.builder().roomType(RoomType.FAMILY).bedType(queenBed).quantity(2).build()
                ));
                System.out.println(">>> [STARTUP] Đã phân bổ cấu trúc giường mặc định cho từng Loại phòng thành công.");
            }
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