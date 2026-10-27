package iuh.fit.se.hotelmanagement_be.modular.room.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.Amenity;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.AmenityRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.AmenityRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.AmenityGetAllResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.services.AmenityService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

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

    @Override
    public AmenityGetAllResponse createAmenity(AmenityRequest request) {
        validateUniqueName(request.getName(), null);
        Amenity amenity = Amenity.builder()
                .name(request.getName().trim())
                .price(request.getPrice())
                .build();
        return toResponse(amenityRepository.save(amenity));
    }

    @Override
    public AmenityGetAllResponse updateAmenity(Long id, AmenityRequest request) {
        Amenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tiện ích."));
        validateUniqueName(request.getName(), id);
        amenity.setName(request.getName().trim());
        amenity.setPrice(request.getPrice());
        return toResponse(amenityRepository.save(amenity));
    }

    private void validateUniqueName(String name, Long excludedId) {
        boolean duplicate = amenityRepository.findAll().stream()
                .anyMatch(existing -> !existing.getId().equals(excludedId)
                        && existing.getName() != null
                        && normalizeName(existing.getName()).equals(normalizeName(name)));
        if (duplicate) {
            throw new IllegalArgumentException("Tên tiện ích đã tồn tại.");
        }
    }

    private AmenityGetAllResponse toResponse(Amenity amenity) {
        return AmenityGetAllResponse.builder()
                .id(amenity.getId())
                .name(amenity.getName())
                .price(amenity.getPrice())
                .build();
    }

    private String normalizeName(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
