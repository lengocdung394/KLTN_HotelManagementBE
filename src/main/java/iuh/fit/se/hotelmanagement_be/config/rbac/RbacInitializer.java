package iuh.fit.se.hotelmanagement_be.config.rbac;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.enums.LoyaltyTier;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Permission;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.CustomerRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.PermissionRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.RoleRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.*;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.*;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.BedType;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomImage;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomTypeBed;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus;
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
    private final CustomerRepository customerRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {

        // ==========================================
        // 0. ĐỒNG BỘ POSTGRESQL SEQUENCES TRÁNH DUPLICATE KEY
        // ==========================================
        try {
            String[] tables = {"orders", "bookings", "booking_details", "booking_services", "payment_transactions", "rooms"};
            String[] pks = {"order_id", "booking_id", "booking_detail_id", "booking_service_id", "payment_transaction_id", "room_id"};
            for (int i = 0; i < tables.length; i++) {
                try {
                    String sql = String.format("SELECT setval(pg_get_serial_sequence('%s', '%s'), COALESCE((SELECT MAX(%s) FROM %s), 0) + 1, false);",
                            tables[i], pks[i], pks[i], tables[i]);
                    jdbcTemplate.execute(sql);
                } catch (Exception ignored) {}
            }
            System.out.println(">>> [STARTUP] Đã đồng bộ tất cả PostgreSQL sequences về MAX(ID) + 1.");
        } catch (Exception e) {
            System.err.println(">>> [STARTUP WARN] Đồng bộ sequence: " + e.getMessage());
        }

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

            branchRoomPolicyRepository.saveAll(List.of(
                    BranchRoomPolicy.builder()
                            .hotel(hotel1)
                            .roomType(RoomType.STANDARD)
                            .standardCapacity(2)    // Sức chứa tiêu chuẩn (VD: 2 người)
                            .maxExtraGuests(2)// Sức chứa phụ thu tối đa (VD: tối đa thêm 2 người)// Số em bé tối đa
                            .extraAdultFee(250000.0)
                            .extraChildFee(120000.0)
                            .basePrice(1000000.0)
                            .build(),
                    BranchRoomPolicy.builder()
                            .hotel(hotel1)
                            .roomType(RoomType.DELUXE)
                            .standardCapacity(2)
                            .maxExtraGuests(3)
                            .extraAdultFee(180000.0)
                            .extraChildFee(90000.0)
                            .basePrice(2000000.0)
                            .build(),
                    BranchRoomPolicy.builder()
                            .hotel(hotel1)
                            .roomType(RoomType.SUITE)
                            .standardCapacity(3)
                            .maxExtraGuests(3)
                            .extraAdultFee(350000.0)
                            .extraChildFee(180000.0)
                            .basePrice(3000000.0)
                            .build(),
                    BranchRoomPolicy.builder()
                            .hotel(hotel1)
                            .roomType(RoomType.FAMILY)
                            .standardCapacity(4)
                            .maxExtraGuests(4)
                            .extraAdultFee(280000.0)
                            .extraChildFee(140000.0)
                            .basePrice(4000000.0)
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

            branchRoomPolicyRepository.saveAll(List.of(
                    BranchRoomPolicy.builder()
                            .hotel(hotel2)
                            .roomType(RoomType.STANDARD)
                            .standardCapacity(2)    // Sức chứa tiêu chuẩn (VD: 2 người)
                            .maxExtraGuests(2)      // Sức chứa phụ thu tối đa (VD: tối đa thêm 2 người)// Số em bé tối đa
                            .extraAdultFee(250000.0)
                            .extraChildFee(120000.0)
                            .basePrice(1000000.0)
                            .build(),
                    BranchRoomPolicy.builder()
                            .hotel(hotel2)
                            .roomType(RoomType.DELUXE)
                            .standardCapacity(2)
                            .maxExtraGuests(3)
                            .extraAdultFee(180000.0)
                            .extraChildFee(90000.0)
                            .basePrice(2000000.0)
                            .build(),
                    BranchRoomPolicy.builder()
                            .hotel(hotel2)
                            .roomType(RoomType.SUITE)
                            .standardCapacity(3)
                            .maxExtraGuests(3)
                            .extraAdultFee(350000.0)
                            .extraChildFee(180000.0)
                            .basePrice(3000000.0)
                            .build(),
                    BranchRoomPolicy.builder()
                            .hotel(hotel2)
                            .roomType(RoomType.FAMILY)
                            .standardCapacity(4)
                            .maxExtraGuests(4)
                            .extraAdultFee(280000.0)
                            .extraChildFee(140000.0)
                            .basePrice(4000000.0)
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

        // ==========================================
        // 6. KHỞI TẠO KHÁCH HÀNG MẪU (NẾU CHƯA CÓ)
        // ==========================================
        try {
            if (customerRepository.count() == 0) {
                Role customerRole = roleRepository.findByName("ROLE_CUSTOMER").orElse(null);
                Account customerAccount = accountRepository.findByEmail("customer@senviet.vn")
                        .orElseGet(() -> accountRepository.save(
                                Account.builder()
                                        .email("customer@senviet.vn")
                                        .password(passwordEncoder.encode("customer123"))
                                        .roles(customerRole != null ? Set.of(customerRole) : Set.of())
                                        .build()
                        ));

                Customer defaultCustomer = Customer.builder()
                        .fullName("Huỳnh Văn Hiếu")
                        .phone("0901234567")
                        .email("customer@senviet.vn")
                        .loyaltyTier(LoyaltyTier.BRONZE)
                        .totalSpent(0.0)
                        .totalBookings(0)
                        .account(customerAccount)
                        .build();
                customerRepository.save(defaultCustomer);
                System.out.println(">>> [STARTUP] Đã khởi tạo Khách hàng mẫu thành công.");
            }
        } catch (Exception e) {
            System.err.println(">>> [STARTUP WARN] Khởi tạo khách hàng mẫu thất bại: " + e.getMessage());
        }

        // ==========================================
        // 7. KHỞI TẠO PHÒNG MẪU (NẾU CHƯA CÓ)
        // ==========================================
        try {
            if (roomRepository.count() == 0) {
                List<Floor> floors = floorRepository.findAll();
                if (!floors.isEmpty()) {
                    List<Room> initialRooms = new ArrayList<>();
                    for (Floor floor : floors) {
                        initialRooms.add(Room.builder()
                                .floor(floor)
                                .roomStatus(RoomStatus.READY)
                                .roomType(RoomType.STANDARD)
                                .price(1000000.0)
                                .basePrice(1000000.0)
                                .avatarUrl(List.of(RoomImage.builder()
                                        .url("https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=900&auto=format&fit=crop")
                                        .isDefault(true).build()))
                                .amenities(Set.of())
                                .build());

                        initialRooms.add(Room.builder()
                                .floor(floor)
                                .roomStatus(RoomStatus.READY)
                                .roomType(RoomType.DELUXE)
                                .price(2000000.0)
                                .basePrice(2000000.0)
                                .avatarUrl(List.of(RoomImage.builder()
                                        .url("https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?q=80&w=900&auto=format&fit=crop")
                                        .isDefault(true).build()))
                                .amenities(Set.of())
                                .build());

                        initialRooms.add(Room.builder()
                                .floor(floor)
                                .roomStatus(RoomStatus.READY)
                                .roomType(RoomType.SUITE)
                                .price(3000000.0)
                                .basePrice(3000000.0)
                                .avatarUrl(List.of(RoomImage.builder()
                                        .url("https://images.unsplash.com/photo-1566665797739-1674de7a421a?q=80&w=900&auto=format&fit=crop")
                                        .isDefault(true).build()))
                                .amenities(Set.of())
                                .build());

                        initialRooms.add(Room.builder()
                                .floor(floor)
                                .roomStatus(RoomStatus.READY)
                                .roomType(RoomType.FAMILY)
                                .price(3500000.0)
                                .basePrice(3500000.0)
                                .avatarUrl(List.of(RoomImage.builder()
                                        .url("https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=900&auto=format&fit=crop")
                                        .isDefault(true).build()))
                                .amenities(Set.of())
                                .build());
                    }
                    roomRepository.saveAll(initialRooms);
                    System.out.println(">>> [STARTUP] Đã khởi tạo phòng mẫu thành công.");
                }
            }
        } catch (Exception e) {
            System.err.println(">>> [STARTUP WARN] Khởi tạo phòng mẫu thất bại: " + e.getMessage());
        }

        // ==========================================
        // 8. ĐẢM BẢO CHÍNH SÁCH GIÁ (POLICY) CHO TẤT CẢ KHÁCH SẠN
        // ==========================================
        try {
            List<Hotel> allHotels = hotelRepository.findAll();
            for (Hotel hotel : allHotels) {
                for (RoomType rt : RoomType.values()) {
                    if (branchRoomPolicyRepository.findByHotelIdAndRoomType(hotel.getId(), rt) == null) {
                        double base = switch (rt) {
                            case STANDARD -> 1000000.0;
                            case DELUXE -> 2000000.0;
                            case SUITE -> 3000000.0;
                            case FAMILY -> 3500000.0;
                        };
                        branchRoomPolicyRepository.save(BranchRoomPolicy.builder()
                                .hotel(hotel)
                                .roomType(rt)
                                .standardCapacity(rt == RoomType.FAMILY ? 4 : (rt == RoomType.SUITE ? 3 : 2))
                                .maxExtraGuests(rt == RoomType.FAMILY ? 4 : (rt == RoomType.DELUXE || rt == RoomType.SUITE ? 3 : 2))
                                .extraAdultFee(200000.0)
                                .extraChildFee(100000.0)
                                .basePrice(base)
                                .build());
                    }
                }
            }
            System.out.println(">>> [STARTUP] Đã đồng bộ BranchRoomPolicy cho toàn bộ khách sạn.");
        } catch (Exception e) {
            System.err.println(">>> [STARTUP WARN] Đồng bộ BranchRoomPolicy thất bại: " + e.getMessage());
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