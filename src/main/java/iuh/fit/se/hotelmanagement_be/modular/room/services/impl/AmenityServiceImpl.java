package iuh.fit.se.hotelmanagement_be.modular.room.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.Amenity;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.AmenityRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.AmenityGetAllResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.services.AmenityService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AmenityServiceImpl implements AmenityService {
    AmenityRepository amenityRepository;

    @Override
    public List<AmenityGetAllResponse> getAllAmenities() {
        // 1. Lấy danh sách Entity từ DB
        List<Amenity> amenities = amenityRepository.findAll();

        // 2. Chuyển đổi từ Entity sang Response DTO (Phải làm bước này để hết lỗi)
        return amenities.stream().map(amenity -> {
            return AmenityGetAllResponse.builder()
                    .id(amenity.getId())          // Lấy ID của amenity
                    .name(amenity.getName())      // Lấy tên amenity
                    .price(amenity.getPrice())    // Lấy giá amenity
                    // Thêm các trường khác nếu trong AmenityGetAllResponse có cấu hình thêm
                    .build();
        }).toList();
    }
}
