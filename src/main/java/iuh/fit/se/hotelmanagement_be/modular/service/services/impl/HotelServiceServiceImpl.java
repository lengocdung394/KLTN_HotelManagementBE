package iuh.fit.se.hotelmanagement_be.modular.service.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import iuh.fit.se.hotelmanagement_be.modular.notification.services.NotificationService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HotelServiceServiceImpl implements HotelServiceService {

    ServiceRepository serviceRepository;
    HotelRepository hotelRepository;
    CloudinaryService cloudinaryService; // 💡 Inject thêm CloudinaryService
    HotelServiceSocketEmitter hotelServiceSocketEmitter;
    NotificationService notificationService;

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

        Service savedService = Service.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .unit(request.getUnit().trim())
                .category(request.getCategory().trim())
                .imageUrl(imageUrl) // 💡 Gán URL ảnh vào Entity
                .active(true)
                .hotel(hotel)
                .build();

        savedService = serviceRepository.save(savedService);
        ServiceResponse response = toResponse(savedService);
        Long savedHotelId = hotel == null ? null : hotel.getId();
        String scopeDescription = savedHotelId == null
                ? "Áp dụng toàn hệ thống."
                : "Áp dụng cho chi nhánh " + hotel.getName() + ".";
        String description = serviceDescription(response) + " " + scopeDescription;
        if (savedHotelId == null) {
            List<Long> targetHotelIds = hotelRepository.findAll().stream().map(Hotel::getId).toList();
            notificationService.createAndPublishSystemWide(
                    NotificationType.SERVICE_CREATED,
                    "Dịch vụ toàn hệ thống mới",
                    description,
                    "SERVICE",
                    response.getId(),
                    targetHotelIds);
            hotelServiceSocketEmitter.emitServiceCreated(response, targetHotelIds);
        } else {
            notificationService.createAndPublishInCurrentTransaction(
                    savedHotelId,
                    NotificationType.SERVICE_CREATED,
                    "Dịch vụ mới được thêm",
                    description,
                    "SERVICE",
                    response.getId());
            hotelServiceSocketEmitter.emitServiceCreated(response, List.of(savedHotelId));
        }
        return response;
    }

    @Override
    @Transactional
    public ServiceResponse updateService(String id, UpdateServiceRequest request, MultipartFile imageFile, Long hotelId) { // 💡 Thêm tham số MultipartFile imageFile
        Service service = findOrThrow(id);
        assertCanManageService(service);

        if (serviceRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
            throw new RuntimeException("Tên dịch vụ '" + request.getName() + "' đã được sử dụng bởi dịch vụ khác!");
        }
        String oldName = service.getName();
        String oldDescription = service.getDescription();
        Double oldPrice = service.getPrice();
        String oldUnit = service.getUnit();
        String oldCategory = service.getCategory();
        Boolean oldActive = service.getActive();
        String oldImageUrl = service.getImageUrl();

        // 💡 Xử lý cập nhật ảnh mới nếu có truyền lên
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl;
            Hotel targetHotel = service.getHotel();

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
        Service savedService = serviceRepository.save(service);
        ServiceResponse response = toResponse(savedService);


        Map<String, String> changes = new LinkedHashMap<>();
        addChange(changes, "Tên dịch vụ", oldName, savedService.getName());
        addChange(changes, "Mô tả", oldDescription, savedService.getDescription());
        addChange(changes, "Giá", oldPrice, savedService.getPrice());
        addChange(changes, "Đơn vị", oldUnit, savedService.getUnit());
        addChange(changes, "Danh mục", oldCategory, savedService.getCategory());
        addChange(changes, "Trạng thái", oldActive, savedService.getActive());
        addChange(changes, "Hình ảnh", oldImageUrl, savedService.getImageUrl());

        publishServiceUpdate(savedService, response, changes);
        return response;
    }

    @Override
    @Transactional
    public void deleteService(String id) {
        Service service = findOrThrow(id);
        assertCanManageService(service);
        Boolean oldActive = service.getActive();
        service.setActive(false);
        Service savedService = serviceRepository.save(service);
        Map<String, String> changes = new LinkedHashMap<>();
        addChange(changes, "Trạng thái", oldActive, savedService.getActive());
        publishServiceUpdate(savedService, toResponse(savedService), changes);
    }

    @Override
    @Transactional
    public ServiceResponse toggleServiceStatus(String id) {
        Service service = findOrThrow(id);
        assertCanManageService(service);
        boolean current = Boolean.TRUE.equals(service.getActive());
        service.setActive(!current);
        Service savedService = serviceRepository.save(service);
        Map<String, String> changes = new LinkedHashMap<>();
        addChange(changes, "Trạng thái", current, savedService.getActive());
        ServiceResponse response = toResponse(savedService);
        publishServiceUpdate(savedService, response, changes);
        return response;
    }

    private void assertCanManageService(Service service) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (isSuperAdmin) return;

        Object principal = authentication.getPrincipal();
        Long serviceHotelId = service.getHotel() == null ? null : service.getHotel().getId();
        if (!(principal instanceof Account account)
                || account.getHotelId() == null
                || !account.getHotelId().equals(serviceHotelId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_BRANCH_ACCESS);
        }
    }

    private void publishServiceUpdate(Service service, ServiceResponse response, Map<String, String> changes) {
        Long serviceHotelId = service.getHotel() == null ? null : service.getHotel().getId();
        String changeDetails = changes.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("; "));
        String description = "Dịch vụ " + serviceDescription(response) + " vừa được chỉnh sửa."
                + (changeDetails.isBlank() ? "" : " Thay đổi: " + changeDetails + ".");
        if (serviceHotelId == null) {
            List<Long> targetHotelIds = hotelRepository.findAll().stream().map(Hotel::getId).toList();
            notificationService.createAndPublishSystemWide(
                    NotificationType.SERVICE_UPDATED,
                    "Dịch vụ toàn hệ thống đã cập nhật",
                    description,
                    "SERVICE",
                    response.getId(),
                    targetHotelIds);
            hotelServiceSocketEmitter.emitServiceUpdated(response, changes, targetHotelIds);
        } else {
            notificationService.createAndPublishInCurrentTransaction(
                    serviceHotelId,
                    NotificationType.SERVICE_UPDATED,
                    "Dịch vụ chi nhánh đã cập nhật",
                    description,
                    "SERVICE",
                    response.getId());
            hotelServiceSocketEmitter.emitServiceUpdated(response, changes, List.of(serviceHotelId));
        }
    }

    private String serviceDescription(ServiceResponse response) {
        return "\"" + response.getName() + "\" (mã " + response.getId() + "), giá "
                + response.getPrice() + "đ/" + response.getUnit()
                + ", danh mục " + response.getCategory()
                + ", trạng thái " + (Boolean.TRUE.equals(response.getActive()) ? "đang hoạt động" : "tạm ngưng") + ".";
    }

    private void addChange(
            Map<String, String> changes,
            String label,
            Object oldValue,
            Object newValue
    ) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.put(
                    label,
                    Objects.toString(oldValue, "trống")
                            + " → "
                            + Objects.toString(newValue, "trống")
            );
        }
    }
}
