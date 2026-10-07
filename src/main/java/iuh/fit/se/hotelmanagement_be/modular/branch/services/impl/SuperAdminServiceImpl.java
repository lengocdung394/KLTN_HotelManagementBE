package iuh.fit.se.hotelmanagement_be.modular.branch.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.RoleRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.EmployeeResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.impl.EmployeeServiceImpl;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponseForHotel;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingService;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Province;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.*;
import iuh.fit.se.hotelmanagement_be.modular.branch.requests.BranchRoomPolicyRequest;
import iuh.fit.se.hotelmanagement_be.modular.branch.requests.SuperAdminCreateBranchRequest;
import iuh.fit.se.hotelmanagement_be.modular.branch.requests.SuperAdminCreateProvinceRequest;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.SuperAdminBranchDetailResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.SuperAdminBranchSummaryResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.SuperAdminHotelResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.SuperAdminProvinceResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.services.SuperAdminService;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.PromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import iuh.fit.se.hotelmanagement_be.modular.service.repositories.ServiceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SuperAdminServiceImpl implements SuperAdminService {

    HotelRepository hotelRepository;
    ProvinceRepository provinceRepository;
    BuildingRepository buildingRepository;
    BranchRoomPolicyRepository branchRoomPolicyRepository;
    FloorRepository floorRepository;
    RoomRepository roomRepository;
    ServiceRepository serviceRepository;
    PromotionRepository promotionRepository;
    EmployeeServiceImpl employeeService;
    BookingService bookingService;
    RoleRepository roleRepository;
    AccountRepository accountRepository;
    EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    // Ham tra ve ds tinh moi tinh - kem theo list khach san
    @Override
    public List<SuperAdminProvinceResponse> getProvinces() {
        return provinceRepository.findAll().stream().map(province ->
                SuperAdminProvinceResponse.builder()
                        .id(province.getId())
                        .name(province.getName())
                        .backgroundImageUrl(province.getBackgroundImageUrl())
                        .hotels(
                                // Map từ List<Hotel> sang List<SuperAdminHotelResponse>
                                province.getHotels().stream()
                                        .map(hotel -> SuperAdminHotelResponse.builder()
                                                .id(hotel.getId())
                                                .name(hotel.getName())
                                                // Thêm các trường khác của Hotel nếu có
                                                .build())
                                        .toList()
                        )
                        .build()
        ).toList();
    }

    @Override
    @Transactional
    public List<SuperAdminBranchDetailResponse.RoomPolicyItem> saveBranchRoomPolicies(
            Long hotelId,
            List<BranchRoomPolicyRequest> requests) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND));

        if (requests == null || requests.isEmpty()) {
            throw new AppException(ErrorCode.ROOM_POLICY_REQUIRED);
        }

        // Nếu bạn muốn ép buộc phải đúng 4 loại phòng như ý bạn đề cập ở trên:
        if (requests.size() != 4) {
            throw new AppException(ErrorCode.INVALID_ROOM_POLICY_COUNT);
        }

        if (requests.stream().anyMatch(request -> request == null || request.getRoomType() == null
                || request.getBasePrice() == null || request.getArea() == null
                || request.getExtraAdultFee() == null || request.getExtraChildFee() == null
                || request.getStandardCapacity() == null || request.getMaxExtraGuests() == null)) {
            throw new AppException(ErrorCode.INVALID_ROOM_POLICY_DATA);
        }

        // Kiểm tra định nghĩa 4 loại phòng bắt buộc (bạn thay các giá trị enum RoomType cho đúng với code thực tế của bạn)
        Set<RoomType> requiredTypes = Set.of(RoomType.STANDARD, RoomType.FAMILY, RoomType.SUITE, RoomType.DELUXE);
        boolean hasAllTypes = requests.stream()
                .map(BranchRoomPolicyRequest::getRoomType)
                .allMatch(requiredTypes::contains);

        if (!hasAllTypes) {
            throw new AppException(ErrorCode.INVALID_ROOM_POLICY_TYPES);
        }

        long distinctRoomTypes = requests.stream().map(BranchRoomPolicyRequest::getRoomType).distinct().count();
        if (distinctRoomTypes != requests.size()) {
            throw new AppException(ErrorCode.DUPLICATE_ROOM_POLICY_TYPE);
        }

        List<BranchRoomPolicy> savedPolicies = requests.stream().map(request -> {
            BranchRoomPolicy policy = branchRoomPolicyRepository
                    .findByHotelAndRoomType(hotel, request.getRoomType())
                    .orElseGet(() -> BranchRoomPolicy.builder()
                            .hotel(hotel)
                            .roomType(request.getRoomType())
                            .build());
            policy.setArea(request.getArea());
            policy.setBasePrice(request.getBasePrice());
            policy.setExtraAdultFee(request.getExtraAdultFee());
            policy.setExtraChildFee(request.getExtraChildFee());
            policy.setStandardCapacity(request.getStandardCapacity());
            policy.setMaxExtraGuests(request.getMaxExtraGuests());
            return branchRoomPolicyRepository.save(policy);
        }).toList();

        return savedPolicies.stream().map(this::toRoomPolicyItem).toList();
    }

    @Override
    public List<SuperAdminBranchSummaryResponse> getBranches() {
        return hotelRepository.findAll().stream().map(this::summarizeBranch).toList();
    }

    @Override
    public List<SuperAdminBranchSummaryResponse> getBranchesByProvince(String provinceId) {
        if (!provinceRepository.existsById(provinceId)) {
            throw new AppException(ErrorCode.PROVINCE_NOT_FOUND);
        }


        return hotelRepository.findAllByProvince_Id(provinceId).stream()
                .map(this::summarizeBranch)
                .toList();
    }

    // ham tao chi nhanh
    @Transactional
    public SuperAdminBranchSummaryResponse createBranch(
            SuperAdminCreateBranchRequest request
    ) {
        inValidation(request);

        Province province = provinceRepository.findByName(request.getProvinceName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy tỉnh/thành: " + request.getProvinceName()
                ));

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException("Chưa cấu hình ROLE_ADMIN"));

        Role managerRole = roleRepository.findByName("ROLE_MANAGER")
                .orElseThrow(() -> new IllegalStateException("Chưa cấu hình ROLE_MANAGER"));

        Hotel hotel = hotelRepository.save(Hotel.builder()
                .name(request.getName().trim())
                .address(request.getAddress().trim())
                .phone(request.getPhone().trim())
                .province(province)
                .build());

        // Account hiện đăng nhập bằng email; username admin cần là email hợp lệ.
        String adminEmail = request.getAdminAccount()
                .getUsername()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (accountRepository.existsByEmail(adminEmail)) {
            throw new IllegalArgumentException("Email tài khoản admin đã tồn tại");
        }

        Account adminAccount = Account.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(
                        request.getAdminAccount().getPassword()
                ))
                .roles(Set.of(adminRole))
                .build();

        Employee adminEmployee = Employee.builder()
                .fullName("Admin - " + hotel.getName())
                .phone(hotel.getPhone())
                .position("Admin chi nhánh")
                .hotel(hotel)
                .account(adminAccount)
                .build();

        employeeRepository.save(adminEmployee);

        String managerEmail = request.getManagerAccount()
                .getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (accountRepository.existsByEmail(managerEmail)) {
            throw new IllegalArgumentException("Email tài khoản quản lý đã tồn tại");
        }

        Account managerAccount = Account.builder()
                .email(managerEmail)
                .password(passwordEncoder.encode(
                        request.getManagerAccount().getPassword()
                ))
                .roles(Set.of(managerRole))
                .build();

        Employee managerEmployee = Employee.builder()
                .fullName(request.getManagerAccount().getFullName().trim())
                .phone(request.getManagerAccount().getPhone().trim())
                .position("Quản lý chi nhánh")
                .hotel(hotel)
                .account(managerAccount)
                .build();

        employeeRepository.save(managerEmployee);

        return summarizeBranch(hotel);
    }

    @Override
    public SuperAdminBranchDetailResponse getBranchDetails(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND));
        List<EmployeeResponse> employees = employeeService.getEmployeesByHotelId(hotelId);
        List<BookingResponseForHotel> bookings = bookingService.getBookingsByHotel(hotelId);
        List<SuperAdminBranchDetailResponse.BuildingItem> buildings = buildingRepository.findByHotelId(hotelId).stream()
                .map(building -> new SuperAdminBranchDetailResponse.BuildingItem(
                        building.getId(),
                        building.getName(),
                        building.getFloors() == null ? 0 : building.getFloors().size()))
                .toList();
        List<SuperAdminBranchDetailResponse.FloorItem> floors = floorRepository.findByBuilding_Hotel_Id(hotelId).stream()
                .map(floor -> new SuperAdminBranchDetailResponse.FloorItem(
                        floor.getId(),
                        floor.getFloorNumber(),
                        floor.getBuilding().getId(),
                        floor.getBuilding().getName(),
                        floor.getRooms() == null ? 0 : floor.getRooms().size()))
                .toList();
        List<SuperAdminBranchDetailResponse.RoomItem> rooms = roomRepository.findByFloor_Building_Hotel_Id(hotelId).stream()
                .map(room -> new SuperAdminBranchDetailResponse.RoomItem(
                        room.getId(),
                        room.getRoomNumber(),
                        room.getRoomType() == null ? null : room.getRoomType().name(),
                        room.getRoomStatus() == null ? null : room.getRoomStatus().name(),
                        room.getFloor().getId(),
                        room.getFloor().getFloorNumber(),
                        room.getFloor().getBuilding().getName()))
                .toList();
        List<SuperAdminBranchDetailResponse.ServiceItem> services = serviceRepository.findAvailableServicesForHotel(hotelId).stream()
                .map(service -> new SuperAdminBranchDetailResponse.ServiceItem(
                        service.getId(),
                        service.getName(),
                        service.getCategory(),
                        service.getPrice(),
                        service.getUnit(),
                        service.getHotel() == null))
                .toList();
        List<SuperAdminBranchDetailResponse.PromotionItem> promotions = promotionRepository.findBranchAndSharedPromotions(hotelId).stream()
                .map(promotion -> new SuperAdminBranchDetailResponse.PromotionItem(
                        promotion.getId(),
                        promotion.getCode(),
                        promotion.getName(),
                        promotion.getStatus() == null ? null : promotion.getStatus().name(),
                        promotion.getStartDate() == null ? null : promotion.getStartDate().toString(),
                        promotion.getEndDate() == null ? null : promotion.getEndDate().toString()))
                .toList();
        List<SuperAdminBranchDetailResponse.RoomPolicyItem> roomPolicies = branchRoomPolicyRepository.findByHotelId(hotelId).stream()
                .map(this::toRoomPolicyItem)
                .toList();

        return SuperAdminBranchDetailResponse.builder()
                .branch(summarizeBranch(hotel, employees.size(), bookings))
                .employees(employees)
                .bookings(bookings)
                .buildings(buildings)
                .floors(floors)
                .rooms(rooms)
                .services(services)
                .promotions(promotions)
                .roomPolicies(roomPolicies)
                .build();
    }

    @Override
    @Transactional
    public SuperAdminProvinceResponse createProvince(SuperAdminCreateProvinceRequest request) {
        // 1. Kiểm tra request hoặc name không được null
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_PROVINCE_NAME); // Hoặc lỗi tương ứng của bạn
        }

        // 2. Khởi tạo Province và truyền đúng name từ request vào
        Province province = Province.builder()
                .id("TEMP") // Dùng tạm để vượt qua check ID null
                .name(request.getName().trim())
                .backgroundImageUrl(request.getBackgroundImageUrl())
                .build();

        Province savedProvince = provinceRepository.save(province);

        // 3. Trả về response
        return SuperAdminProvinceResponse.builder()
                .id(savedProvince.getId())
                .name(savedProvince.getName())
                .backgroundImageUrl(
                        savedProvince.getBackgroundImageUrl()
                )
                .hotels(List.of())
                .build();
    }

    private SuperAdminBranchDetailResponse.RoomPolicyItem toRoomPolicyItem(BranchRoomPolicy policy) {
        return new SuperAdminBranchDetailResponse.RoomPolicyItem(
                policy.getId(),
                policy.getRoomType().name(),
                policy.getArea(),
                policy.getBasePrice(),
                policy.getExtraAdultFee(),
                policy.getExtraChildFee(),
                policy.getStandardCapacity(),
                policy.getMaxExtraGuests());
    }

    private SuperAdminBranchSummaryResponse summarizeBranch(Hotel hotel) {
        List<BookingResponseForHotel> bookings = bookingService.getBookingsByHotel(hotel.getId());
        long employeeCount = employeeService.getEmployeesByHotelId(hotel.getId()).size();
        return summarizeBranch(hotel, employeeCount, bookings);
    }

    private SuperAdminBranchSummaryResponse summarizeBranch(
            Hotel hotel,
            long employeeCount,
            List<BookingResponseForHotel> bookings) {
        BigDecimal totalRevenue = bookings.stream()
                .map(BookingResponseForHotel::getPaidAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return SuperAdminBranchSummaryResponse.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .address(hotel.getAddress())
                .phone(hotel.getPhone())
                .provinceName(hotel.getProvince() == null ? null : hotel.getProvince().getName())
                .employeeCount(employeeCount)
                .bookingCount(bookings.size())
                .totalRevenue(totalRevenue)
                .build();
    }


    private void inValidation(SuperAdminCreateBranchRequest request) {

        // 1. Kiểm tra rỗng và định dạng cơ bản
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_BRANCH_NAME);
        }

        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ADDRESS);
        }

        if (request.getPhone() == null || !request.getPhone().matches("^0[0-9]{9,10}$")) {
            throw new AppException(ErrorCode.INVALID_PHONE);
        }

        if (request.getProvinceName() == null || request.getProvinceName().trim().isEmpty() || !provinceRepository.findByName(request.getProvinceName()).isPresent()) {
            throw new AppException(ErrorCode.INVALID_PROVINCE_NAME);
        }


        // --- 2. KIỂM TRA TRÙNG LẶP TRONG DATABASE ---

        // Kiểm tra tên chi nhánh đã tồn tại chưa
        if (hotelRepository.existsByName(request.getName().trim())) {
            throw new AppException(ErrorCode.BRANCH_EXISTED);
        }

        // Kiểm tra số điện thoại đã tồn tại chưa
        if (hotelRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_EXISTED);
        }

        // Kiểm tra địa chỉ đã tồn tại chưa
        if (hotelRepository.existsByAddress(request.getAddress().trim())) {
            throw new AppException(ErrorCode.ADDRESS_EXISTED);
        }

    }


}
