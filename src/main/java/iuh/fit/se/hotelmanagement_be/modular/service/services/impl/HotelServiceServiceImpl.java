package iuh.fit.se.hotelmanagement_be.modular.service.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.service.entities.Service;
import iuh.fit.se.hotelmanagement_be.modular.service.repositories.ServiceRepository;
import iuh.fit.se.hotelmanagement_be.modular.service.requests.CreateServiceRequest;
import iuh.fit.se.hotelmanagement_be.modular.service.requests.UpdateServiceRequest;
import iuh.fit.se.hotelmanagement_be.modular.service.responses.ServiceResponse;
import iuh.fit.se.hotelmanagement_be.modular.service.services.HotelServiceService;
import iuh.fit.se.hotelmanagement_be.shared.CloudinaryService;
import iuh.fit.se.hotelmanagement_be.shared.enums.ImageCategory;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HotelServiceServiceImpl implements HotelServiceService {

    ServiceRepository serviceRepository;
    HotelRepository hotelRepository;
    CloudinaryService cloudinaryService; // 💡 Inject thêm CloudinaryService

    private ServiceResponse toResponse(Service service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .unit(service.getUnit())
                .category(service.getCategory())
                .imageUrl(service.getImageUrl())
                .active(service.getActive())
                .hotelId(service.getHotel() != null ? service.getHotel().getId() : null)
                .hotelName(service.getHotel() != null ? service.getHotel().getName() : "Toàn chuỗi")
                .build();
    }

    private Service findOrThrow(String id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ với ID: " + id));
    }

    @Override
    public List<ServiceResponse> getAllServices(Long hotelId, String category, Boolean activeOnly) {
        List<Service> list;

        if (hotelId != null) {
            list = serviceRepository.findAvailableServicesForHotel(hotelId);
        } else {
            list = serviceRepository.findAll();
        }

        return list.stream()
                .filter(s -> activeOnly == null || !activeOnly || Boolean.TRUE.equals(s.getActive()))
                .filter(s -> category == null || category.isBlank() || (s.getCategory() != null && s.getCategory().equalsIgnoreCase(category.trim())))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ServiceResponse getServiceById(String id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public ServiceResponse createService(CreateServiceRequest request, MultipartFile imageFile, Long hotelId) { // 💡 Thêm tham số MultipartFile imageFile
        if (serviceRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new RuntimeException("Tên dịch vụ '" + request.getName() + "' đã tồn tại trong hệ thống!");
        }

        Hotel hotel = null;
        if (hotelId != null) {
            hotel = hotelRepository.findById(hotelId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn với ID: " + hotelId));
        }

        // 💡 Xử lý upload ảnh dịch vụ lên Cloudinary
        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            if (hotel != null && hotel.getName() != null) {
                // Nếu dịch vụ thuộc chi nhánh cụ thể -> Lưu vào thư mục của chi nhánh
                List<String> uploadedUrls = cloudinaryService.uploadBranchImages(List.of(imageFile), hotel.getName(), ImageCategory.SERVICES);
                imageUrl = uploadedUrls.isEmpty() ? null : uploadedUrls.get(0);
            } else {
                // Nếu là dịch vụ chung toàn chuỗi -> Lưu vào system/services
                imageUrl = cloudinaryService.uploadImage(imageFile, "system/services");
            }
        }

        Service service = Service.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .unit(request.getUnit().trim())
                .category(request.getCategory().trim())
                .imageUrl(imageUrl) // 💡 Gán URL ảnh vào Entity
                .active(true)
                .hotel(hotel)
                .build();

        return toResponse(serviceRepository.save(service));
    }

    @Override
    @Transactional
    public ServiceResponse updateService(String id, UpdateServiceRequest request, MultipartFile imageFile, Long hotelId) { // 💡 Thêm tham số MultipartFile imageFile
        Service service = findOrThrow(id);

        if (serviceRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
            throw new RuntimeException("Tên dịch vụ '" + request.getName() + "' đã được sử dụng bởi dịch vụ khác!");
        }

        Hotel hotel = null;
        if (hotelId != null) {
            hotel = hotelRepository.findById(hotelId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn với ID: " + hotelId));
        }

        // 💡 Xử lý cập nhật ảnh mới nếu có truyền lên
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl;
            Hotel targetHotel = hotel != null ? hotel : service.getHotel();

            if (targetHotel != null && targetHotel.getName() != null) {
                List<String> uploadedUrls = cloudinaryService.uploadBranchImages(List.of(imageFile), targetHotel.getName(), ImageCategory.SERVICES);
                imageUrl = uploadedUrls.isEmpty() ? null : uploadedUrls.get(0);
            } else {
                imageUrl = cloudinaryService.uploadImage(imageFile, "system/services");
            }
            service.setImageUrl(imageUrl);
        }

        service.setName(request.getName().trim());
        service.setDescription(request.getDescription());
        service.setPrice(request.getPrice());
        service.setUnit(request.getUnit().trim());
        service.setCategory(request.getCategory().trim());
        if (request.getActive() != null) {
            service.setActive(request.getActive());
        }
        service.setHotel(hotel);

        return toResponse(serviceRepository.save(service));
    }

    @Override
    @Transactional
    public void deleteService(String id) {
        Service service = findOrThrow(id);
        service.setActive(false);
        serviceRepository.save(service);
    }

    @Override
    @Transactional
    public ServiceResponse toggleServiceStatus(String id) {
        Service service = findOrThrow(id);
        boolean current = Boolean.TRUE.equals(service.getActive());
        service.setActive(!current);
        return toResponse(serviceRepository.save(service));
    }
}
