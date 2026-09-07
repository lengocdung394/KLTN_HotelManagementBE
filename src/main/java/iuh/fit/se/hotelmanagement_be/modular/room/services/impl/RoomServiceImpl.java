package iuh.fit.se.hotelmanagement_be.modular.room.services.impl;

import iuh.fit.se.hotelmanagement_be.config.SecurityUtils;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Floor;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.FloorRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Amenity;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomImage;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.AmenityRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.RoomCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomCreateResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.services.RoomService;
import iuh.fit.se.hotelmanagement_be.shared.CloudinaryService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomServiceImpl implements RoomService {
    RoomRepository roomRepository;
    FloorRepository floorRepository;
    AmenityRepository amenityRepository;
    CloudinaryService cloudinaryService;

    @Transactional
    @Override
    public RoomCreateResponse createRoom(RoomCreateRequest dto, List<MultipartFile> imageFiles) {

        // 1. kiem tra ds file upload
        if (imageFiles == null || imageFiles.isEmpty() || imageFiles.stream().allMatch(f -> f.isEmpty() || f == null))
            throw new RuntimeException(
                    "Vui long dang tai anh"
            );

        //2. Kiem tra tang floor
        Floor floor = floorRepository.findById(dto.getFloorId()).orElseThrow(() -> new RuntimeException(" Khong tim thay tang"));

        //3. Upload anh len cloudinary
        List<String> uploadedUrls = cloudinaryService.uploadMultipleImages(imageFiles, "room");

        // 4. Xac dinh vi tri anh dai dien
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
}
