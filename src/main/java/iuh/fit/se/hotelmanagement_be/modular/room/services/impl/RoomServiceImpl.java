package iuh.fit.se.hotelmanagement_be.modular.room.services.impl;

import iuh.fit.se.hotelmanagement_be.config.SecurityUtils;
import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Floor;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.FloorRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Amenity;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomImage;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomTypeBed;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.AmenityRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomTypeBedRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.RoomCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomBedResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomCreateResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomTypeDetailResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.services.RoomService;
import iuh.fit.se.hotelmanagement_be.shared.CloudinaryService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomServiceImpl implements RoomService {
    RoomRepository roomRepository;
    FloorRepository floorRepository;
    AmenityRepository amenityRepository;
    CloudinaryService cloudinaryService;
    RoomTypeBedRepository roomTypeBedRepository;
    private final BranchRoomPolicyRepository branchRoomPolicyRepository;
    private final HotelRepository hotelRepository;

    @Transactional
    @Override
    public RoomCreateResponse createRoom(RoomCreateRequest dto, List<MultipartFile> imageFiles) {

        log.info("Thêm phòng, mã: {}", dto.toString());
        // 1. Kiểm tra danh sách file upload (Bắt buộc từ 4 đến 8 ảnh)
        if (imageFiles == null || imageFiles.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_IMAGE_COUNT);
        }

        // Lọc bỏ các file rỗng (nếu có)
        List<MultipartFile> validFiles = imageFiles.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        if (validFiles.size() < 4 || validFiles.size() > 8) {
            throw new AppException(ErrorCode.INVALID_IMAGE_COUNT);
        }

        // 2. Tìm Tầng (Floor)
        Floor floor = floorRepository.findById(dto.getFloorId())
                .orElseThrow(() -> new AppException(ErrorCode.FLOOR_NOT_FOUND));

        // ==================== BỔ SUNG: XÁC THỰC CHI NHÁNH (HOTEL SCOPE) ====================
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        var authorities = authentication.getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isManager = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        // Lấy hotelId của tài khoản đang đăng nhập
        Long userHotelId = null;
        if (authentication.getPrincipal() instanceof Account account) {
            userHotelId = account.getHotelId();
        }

        // Lấy hotelId mà Floor này đang thuộc về (Floor -> Building -> Hotel)
        Long floorHotelId = (floor.getBuilding() != null && floor.getBuilding().getHotel() != null)
                ? floor.getBuilding().getHotel().getId()
                : null;

        // Rào chắn bảo mật: Nếu là Manager hoặc Admin đã được gán chi nhánh cố định
        if (isManager || (isAdmin && userHotelId != null)) {
            if (userHotelId == null) {
                throw new AppException(ErrorCode.MANAGER_HOTEL_NOT_ASSIGNED);
            }
            // NẾU TẦNG KHÔNG THUỘC KHÁCH SẠN CỦA USER -> BÁO LỖI UNAUTHORIZED
            if (!userHotelId.equals(floorHotelId)) {
                throw new AppException(ErrorCode.UNAUTHORIZED); // Hoặc ErrorCode.CANNOT_CREATE_ROOM_FOR_OTHER_HOTEL
            }
        }
        // ===================================================================================

        // 3. Upload ảnh lên Cloudinary
        List<String> uploadedUrls = cloudinaryService.uploadMultipleImages(imageFiles, "room");

        // 4. Xác định vị trí ảnh đại diện
        int targetDefaultIndex = 0;
        if (dto.getDefaultImageIndex() != null
                && dto.getDefaultImageIndex() >= 0
                && dto.getDefaultImageIndex() < uploadedUrls.size()) {
            targetDefaultIndex = dto.getDefaultImageIndex();
        }

        // 5. Build danh sách RoomImage (gắn cờ isDefault)
        List<RoomImage> roomImages = new ArrayList<>();
        for (int i = 0; i < uploadedUrls.size(); i++) {
            boolean isDefault = (i == targetDefaultIndex);
            roomImages.add(RoomImage.builder()
                    .url(uploadedUrls.get(i))
                    .isDefault(isDefault)
                    .build());
        }

        // 6. Lấy danh sách Tiện ích (Amenities) nếu có
        Set<Amenity> amenities = new HashSet<>();
        if (dto.getAmenityIds() != null && !dto.getAmenityIds().isEmpty()) {
            amenities = new HashSet<>(amenityRepository.findAllById(dto.getAmenityIds()));
        }

        // 7. Tạo Entity và Lưu xuống CSDL
        Room newRoom = Room.builder()
                .floor(floor)
                .roomStatus(dto.getRoomStatus())
                .roomType(dto.getRoomType())
                .basePrice(dto.getBasePrice())
                .avatarUrl(roomImages)
                .amenities(amenities)
                .build();

        Room savedRoom = roomRepository.save(newRoom);

        // 8. Chuyển đổi sang Response
        return RoomCreateResponse.builder()
                .id(savedRoom.getId()) // Nên trả về cả ID phòng vừa tạo
                .floorId(savedRoom.getFloor().getId())
                .roomStatus(savedRoom.getRoomStatus())
                .roomType(savedRoom.getRoomType())
                .basePrice(savedRoom.getBasePrice())
                .totalAmenitiesPrice(savedRoom.getTotalAmenitiesPrice())
                .totalPrice(savedRoom.calculateTotalPrice())
                .defaultImageUrl(savedRoom.getDefaultImageUrl())
                .avatarUrl(savedRoom.getAvatarUrl())
                .amenities(savedRoom.getAmenities())
                .build();
    }

    @Override
    public List<RoomResponse> getRoomsByFloorId(Long floorId) {
        Floor floor = floorRepository.findById(floorId)
                .orElseThrow(() -> new RuntimeException("Floor not found"));

        // Lấy hotelId của User đang login từ SecurityContext
        Long currentHotelId = SecurityUtils.getCurrentUserHotelId();

        // Nếu không phải Super Admin VÀ tầng này không thuộc khách sạn của User -> Báo lỗi Access Denied
        if (currentHotelId != null && !floor.getBuilding().getHotel().getId().equals(currentHotelId)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập dữ liệu tầng của chi nhánh khác!");
        }

        return roomRepository.findByFloorId(floorId).stream()
                .map(r -> RoomResponse.builder()
                        .id(r.getId())
                        .floorId(r.getFloor().getId())
                        .roomStatus(r.getRoomStatus())
                        .roomType(r.getRoomType())
                        .basePrice(r.getBasePrice())
                        .totalAmenitiesPrice(r.getTotalAmenitiesPrice())
                        .totalPrice(r.calculateTotalPrice())
                        .defaultImageUrl(r.getDefaultImageUrl())
                        .avatarUrl(r.getAvatarUrl())
                        .amenities(r.getAmenities()).build()
                )
                .collect(Collectors.toList());
    }

    @Override
    public List<RoomResponse> getRoomsByHotelId(Long hotelId) {
        List<Room> rooms = roomRepository.findByFloor_Building_Hotel_Id(hotelId);
        if (rooms == null || rooms.isEmpty()) {
            log.warn("⚠ Không tìm thấy phòng nào cho hotelId: {}", hotelId);
            return Collections.emptyList();
        }

        return rooms.stream().map(room -> {
            // 1. Lấy Hotel của phòng thông qua chuỗi quan hệ Floor -> Building -> Hotel
            Hotel hotel = room.getFloor().getBuilding().getHotel();

            // 2. Tìm BranchRoomPolicy dựa vào Hotel và RoomType của phòng
            BranchRoomPolicy policy = branchRoomPolicyRepository
                    .findByHotelAndRoomType(hotel, room.getRoomType())
                    .orElse(null);

            // 3. Lấy danh sách giường theo RoomType (như phần trước)
            List<RoomTypeBed> roomTypeBeds = roomTypeBedRepository.findByRoomType(room.getRoomType());
            List<RoomBedResponse> bedResponses = roomTypeBeds.stream().map(rtb ->
                    RoomBedResponse.builder()
                            .bedTypeName(rtb.getBedType().getName())
                            .description(rtb.getBedType().getDescription())
                            .quantity(rtb.getQuantity())
                            .capacity(rtb.getBedType().getCapacity())
                            .isExtraBed(rtb.getBedType().getIsExtraBed())
                            .build()
            ).toList();

            // 4. Build ra RoomResponse đầy đủ thông tin chính sách phòng
            return RoomResponse.builder()
                    .id(room.getId())
                    .floorId(room.getFloor() != null ? room.getFloor().getId() : null)
                    .floorNumber(room.getFloor().getFloorNumber())
                    .nameBuilding(room.getFloor().getBuilding().getName())
                    .basePrice(room.getBasePrice())
                    .roomStatus(room.getRoomStatus())
                    .roomType(room.getRoomType())
                    .avatarUrl(room.getAvatarUrl())
                    .amenities(room.getAmenities())
                    .totalPrice(room.calculateTotalPrice())
                    .defaultImageUrl(room.getDefaultImageUrl())
                    .beds(bedResponses)
                    // Đưa thông tin từ BranchRoomPolicy vào Response (nếu có tồn tại)
                    .standardAdults(policy != null ? policy.getStandardAdults() : null)
                    .maxAdults(policy != null ? policy.getMaxAdults() : null)
                    .maxChildren(policy != null ? policy.getMaxChildren() : null)
                    .maxInfants(policy != null ? policy.getMaxInfants() : null)
                    .extraAdultFee(policy != null ? policy.getExtraAdultFee() : null)
                    .extraChildFee(policy != null ? policy.getExtraChildFee() : null)
                    .build();
        }).toList();
    }

    @Override
    public RoomTypeDetailResponse getRoomTypeDetailByHotelAndType(Long hotelId, RoomType roomType) {
        // 1. Lấy thông tin Hotel từ hotelId
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn với ID: " + hotelId));

        // 2. Lấy Chính sách quy định của loại phòng tại khách sạn này
        BranchRoomPolicy policy = branchRoomPolicyRepository
                .findByHotelAndRoomType(hotel, roomType)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chính sách cho loại phòng này tại chi nhánh."));

        // 3. Lấy Danh sách giường đi kèm của loại phòng này
        List<RoomTypeBed> roomTypeBeds = roomTypeBedRepository.findByRoomType(roomType);

        List<RoomBedResponse> bedResponses = roomTypeBeds.stream().map(rtb ->
                RoomBedResponse.builder()
                        .bedTypeName(rtb.getBedType().getName())
                        .description(rtb.getBedType().getDescription())
                        .quantity(rtb.getQuantity())
                        .capacity(rtb.getBedType().getCapacity())
                        .isExtraBed(rtb.getBedType().getIsExtraBed())
                        .build()
        ).toList();

        // 4. Tổng hợp và trả về DTO
        return RoomTypeDetailResponse.builder()
                .roomType(roomType)
                .standardAdults(policy.getStandardAdults())
                .maxAdults(policy.getMaxAdults())
                .maxChildren(policy.getMaxChildren())
                .maxInfants(policy.getMaxInfants())
                .extraAdultFee(policy.getExtraAdultFee())
                .extraChildFee(policy.getExtraChildFee())
                .beds(bedResponses)
                .build();
    }
}
