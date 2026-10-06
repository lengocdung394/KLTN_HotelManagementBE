package iuh.fit.se.hotelmanagement_be.modular.room.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.Amenity;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.AmenityRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.requestForAmenityExcel.AmenityExcelImportRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.requestForAmenityExcel.AmenityExcelRawRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.AmenityGetAllResponse;
import iuh.fit.se.hotelmanagement_be.shared.entities.ImportTaskStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AmenityExcelService implements iuh.fit.se.hotelmanagement_be.modular.room.services.AmenityExcelService {
    AmenityRepository amenityRepository;
    Map<String, ImportTaskStatus> taskStatusMap = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public List<AmenityGetAllResponse> importAmenities(AmenityExcelImportRequest request) {
        List<Amenity> amenitiesToSave = prepareAmenities(request);
        return saveAmenities(amenitiesToSave);
    }

    @Override
    public String startAsyncImport(AmenityExcelImportRequest request) {
        validateRequest(request);
        String taskId = UUID.randomUUID().toString();
        taskStatusMap.put(taskId, new ImportTaskStatus(0, "Đang khởi tạo tiến trình nhập tiện nghi...", "PROCESSING"));
        CompletableFuture.runAsync(() -> processImportTask(taskId, request));
        return taskId;
    }

    @Override
    public ImportTaskStatus getImportStatus(String taskId) {
        return taskStatusMap.getOrDefault(taskId, new ImportTaskStatus(0, "Không tìm thấy tiến trình.", "NOT_FOUND"));
    }

    private void processImportTask(String taskId, AmenityExcelImportRequest request) {
        try {
            validateRequest(request);
            List<Amenity> amenitiesToSave = prepareAmenities(request, taskId);
            taskStatusMap.put(taskId, new ImportTaskStatus(90, "Đang lưu tiện nghi vào cơ sở dữ liệu...", "PROCESSING"));
            List<AmenityGetAllResponse> imported = saveAmenities(amenitiesToSave);
            taskStatusMap.put(taskId, new ImportTaskStatus(100,
                    "Đã lưu " + imported.size() + " tiện nghi mới.", "SUCCESS", imported));
        } catch (Exception error) {
            log.error("Amenity import task {} failed", taskId, error);
            taskStatusMap.put(taskId, new ImportTaskStatus(0,
                    "Không thể nhập tiện nghi: " + error.getMessage(), "FAILED"));
        }
    }

    private List<Amenity> prepareAmenities(AmenityExcelImportRequest request) {
        return prepareAmenities(request, null);
    }

    private List<Amenity> prepareAmenities(AmenityExcelImportRequest request, String taskId) {
        validateRequest(request);
        List<Amenity> amenitiesToSave = new ArrayList<>();
        Set<String> seenNames = new HashSet<>(amenityRepository.findAll().stream()
                .map(Amenity::getName)
                .filter(name -> name != null)
                .map(this::normalizeName)
                .toList());
        List<AmenityExcelRawRequest> rows = request.getAmenities();
        for (int index = 0; index < rows.size(); index++) {
            AmenityExcelRawRequest row = rows.get(index);
            if (row == null || row.getName() == null || row.getName().isBlank()
                    || row.getPrice() == null || !Double.isFinite(row.getPrice()) || row.getPrice() < 0) {
                int rowNumber = row == null || row.getRow() == null ? 0 : row.getRow();
                throw new IllegalArgumentException("Dòng " + rowNumber + " thiếu tên tiện nghi hoặc có giá không hợp lệ.");
            }

            String name = row.getName().trim();
            if (seenNames.add(normalizeName(name))) {
                amenitiesToSave.add(Amenity.builder()
                        .name(name)
                        .price(row.getPrice())
                        .build());
            }
            if (taskId != null) {
                int percent = 10 + (int) (((index + 1d) / rows.size()) * 75);
                taskStatusMap.put(taskId, new ImportTaskStatus(percent,
                        "Đang kiểm tra tiện nghi (" + (index + 1) + "/" + rows.size() + ").", "PROCESSING"));
            }
        }
        return amenitiesToSave;
    }

    private List<AmenityGetAllResponse> saveAmenities(List<Amenity> amenitiesToSave) {
        if (amenitiesToSave.isEmpty()) {
            return List.of();
        }

        return amenityRepository.saveAll(amenitiesToSave).stream()
                .map(amenity -> AmenityGetAllResponse.builder()
                        .id(amenity.getId())
                        .name(amenity.getName())
                        .price(amenity.getPrice())
                        .build())
                .toList();
    }

    private void validateRequest(AmenityExcelImportRequest request) {
        if (request == null || request.getAmenities() == null || request.getAmenities().isEmpty()) {
            throw new IllegalArgumentException("File không có dòng tiện nghi để nhập.");
        }
    }

    private String normalizeName(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
