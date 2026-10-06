package iuh.fit.se.hotelmanagement_be.modular.service.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.service.entities.Service;
import iuh.fit.se.hotelmanagement_be.modular.service.repositories.ServiceRepository;
import iuh.fit.se.hotelmanagement_be.modular.service.requests.requestForExcel.ServiceExcelRowRequest;
import iuh.fit.se.hotelmanagement_be.shared.entities.ImportTaskStatus;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@org.springframework.stereotype.Service
public class ServiceImportService {
    private final ServiceRepository serviceRepository;
    private final HotelRepository hotelRepository;
    private final ServiceImportSocketEmitter serviceImportSocketEmitter;
    private final Map<String, ImportTaskStatus> taskStatusMap = new ConcurrentHashMap<>();

    public ServiceImportService(
            ServiceRepository serviceRepository,
            HotelRepository hotelRepository,
            ServiceImportSocketEmitter serviceImportSocketEmitter) {
        this.serviceRepository = serviceRepository;
        this.hotelRepository = hotelRepository;
        this.serviceImportSocketEmitter = serviceImportSocketEmitter;
    }

    public ImportTaskStatus getTaskStatus(String taskId) {
        return taskStatusMap.getOrDefault(taskId, new ImportTaskStatus(0, "Không tìm thấy tiến trình", "NOT_FOUND"));
    }

    public String startAsyncUrlImport(iuh.fit.se.hotelmanagement_be.modular.service.requests.requestForExcel.ServiceExcelImportRequest
                                              request, Long hotelId) {
        if (request == null || request.getServices() == null || request.getServices().isEmpty()) {
            throw new IllegalArgumentException("Không có dữ liệu dịch vụ để nhập.");
        }
        if (hotelId == null) {
            throw new IllegalArgumentException("Tài khoản chưa được gán chi nhánh.");
        }

        Set<String> names = new HashSet<>();
        for (ServiceExcelRowRequest row : request.getServices()) {
            if (row == null || isBlank(row.getName()) || isBlank(row.getDescription())
                    || isBlank(row.getUnit()) || isBlank(row.getCategory())
                    || row.getPrice() == null || !Double.isFinite(row.getPrice()) || row.getPrice() < 0) {
                throw new IllegalArgumentException("Dữ liệu dịch vụ có thông tin bắt buộc bị thiếu hoặc không hợp lệ.");
            }
            if (!isCloudinaryImageUrl(row.getImageUrl())) {
                throw new IllegalArgumentException("URL ảnh dịch vụ phải là URL HTTPS hợp lệ từ Cloudinary.");
            }
            String name = row.getName().trim();
            if (!names.add(name.toLowerCase(Locale.ROOT)) || serviceRepository.existsByNameIgnoreCase(name)) {
                throw new IllegalArgumentException("Tên dịch vụ \"" + name + "\" bị trùng.");
            }
        }

        String taskId = UUID.randomUUID().toString();
        updateStatus(taskId, hotelId, 0, "Đang khởi tạo...", "PROCESSING", false);
        CompletableFuture.runAsync(() -> processUrlImportTask(taskId, request.getServices(), hotelId));
        return taskId;
    }

    private void processUrlImportTask(String taskId, List<ServiceExcelRowRequest> rows, Long hotelId) {
        try {
            updateStatus(taskId, hotelId, 10, "Đang xác nhận chi nhánh...", "PROCESSING", false);
            Hotel hotel = hotelRepository.findById(hotelId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh của tài khoản."));
            List<Service> servicesToSave = new ArrayList<>();
            for (int index = 0; index < rows.size(); index++) {
                ServiceExcelRowRequest row = rows.get(index);
                servicesToSave.add(Service.builder()
                        .name(row.getName().trim())
                        .description(row.getDescription().trim())
                        .price(row.getPrice())
                        .unit(row.getUnit().trim())
                        .category(row.getCategory().trim())
                        .imageUrl(row.getImageUrl().trim())
                        .active(true)
                        .hotel(hotel)
                        .build());
                int percent = 10 + (int) (((index + 1d) / rows.size()) * 75);
                updateStatus(taskId, hotelId, percent,
                        "Đang ánh xạ dịch vụ: " + (index + 1) + "/" + rows.size(),
                        "PROCESSING", false);
            }

            updateStatus(taskId, hotelId, 90, "Đang lưu dịch vụ vào cơ sở dữ liệu...", "PROCESSING", false);
            serviceRepository.saveAll(servicesToSave);
            completeTask(taskId, hotelId, "Đã nhập thành công " + servicesToSave.size() + " dịch vụ.");
        } catch (Exception e) {
            failTask(taskId, hotelId, "Không thể nhập dịch vụ: " + e.getMessage());
        }
    }

    private boolean isCloudinaryImageUrl(String value) {
        if (isBlank(value)) return false;
        try {
            URI uri = URI.create(value.trim());
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "res.cloudinary.com".equalsIgnoreCase(uri.getHost())
                    && uri.getPath() != null
                    && uri.getPath().contains("/image/upload/");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void updateStatus(
            String taskId,
            Long hotelId,
            int percent,
            String message,
            String status,
            boolean completed) {
        taskStatusMap.put(taskId, new ImportTaskStatus(percent, message, "PROCESSING"));
        serviceImportSocketEmitter.emitImportProgress(hotelId, taskId, percent, message, status, completed);
    }

    private void completeTask(String taskId, Long hotelId, String message) {
        taskStatusMap.put(taskId, new ImportTaskStatus(100, message, "SUCCESS"));
        serviceImportSocketEmitter.emitImportProgress(hotelId, taskId, 100, message, "SUCCESS", true);
    }

    private void failTask(String taskId, Long hotelId, String errorMsg) {
        taskStatusMap.put(taskId, new ImportTaskStatus(0, errorMsg, "FAILED"));
        serviceImportSocketEmitter.emitImportProgress(hotelId, taskId, 0, errorMsg, "FAILED", true);
    }
}
