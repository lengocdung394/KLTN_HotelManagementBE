package iuh.fit.se.hotelmanagement_be.modular.room.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Floor;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.FloorRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.*;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.AmenityRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.BedTypeRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomTypeBedRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.RoomBedRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.requestForRoomExcel.RoomExcelImportRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.requestForRoomExcel.RoomExcelRawRequest;
import iuh.fit.se.hotelmanagement_be.shared.entities.ImportTaskStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomExcelService {
    FloorRepository floorRepository;
    AmenityRepository amenityRepository;
    BedTypeRepository bedTypeRepository;
    RoomRepository roomRepository;
    RoomTypeBedRepository roomBedRepository;
    BranchRoomPolicyRepository branchRoomPolicyRepository;
    PlatformTransactionManager transactionManager;
    RoomSocketEmitter roomSocketEmitter;
    RoomImportSocketEmitter roomImportSocketEmitter;

    Map<String, ImportTaskStatus> taskStatusMap = new ConcurrentHashMap<>();

    public String startAsyncRoomImport(RoomExcelImportRequest request, Long hotelId) {
        if (request == null || request.getRooms() == null || request.getRooms().isEmpty()) {
            throw new IllegalArgumentException("Không có dữ liệu phòng để nhập.");
        }

        String taskId = UUID.randomUUID().toString();
        taskStatusMap.put(taskId, new ImportTaskStatus(0, "Đang khởi tạo tiến trình nhập phòng...", "PROCESSING"));
        roomImportSocketEmitter.emitImportProgress(
                hotelId, taskId, 0, "Đang khởi tạo tiến trình nhập phòng...", "PROCESSING", List.of(), false);
        CompletableFuture.runAsync(() -> processRoomImportTask(taskId, request, hotelId));
        return taskId;
    }

    public ImportTaskStatus getTaskStatus(String taskId) {
        return taskStatusMap.getOrDefault(taskId, new ImportTaskStatus(0, "Không tìm thấy tiến trình.", "NOT_FOUND"));
    }

    private void processRoomImportTask(String taskId, RoomExcelImportRequest request, Long hotelId) {
        List<Map<String, Object>> details = new ArrayList<>();
        List<RoomImportEntry> roomsToSave = new ArrayList<>();
        Map<Long, BedType> bedTypesById = new HashMap<>();
        Set<String> roomKeys = new HashSet<>();
        int total = request.getRooms().size();

        taskStatusMap.put(taskId, new ImportTaskStatus(5, "Đang kiểm tra URL ảnh và dữ liệu phòng...", "PROCESSING"));
        roomImportSocketEmitter.emitImportProgress(
                hotelId, taskId, 5, "Đang kiểm tra URL ảnh và dữ liệu phòng...", "PROCESSING", details, false);
        try {
            for (int index = 0; index < total; index++) {
                RoomExcelRawRequest item = request.getRooms().get(index);
                int rowNumber = item.getRowNumber() == null ? index + 2 : item.getRowNumber();
                String roomNumber = item.getRoomNumber() == null ? "" : item.getRoomNumber().trim();

                log.info("🔍 [IMPORT CHECK] Phòng số: {}, Dòng Excel: {}, Số lượng loại giường FE gửi lên: {}",
                        roomNumber,
                        rowNumber,
                        item.getBeds() != null ? item.getBeds().size() : "null");
                try {
                    if (roomNumber.isBlank()) throw new IllegalArgumentException("Thiếu số phòng.");
                    if (item.getFloorId() == null || item.getFloorId().isBlank()) throw new IllegalArgumentException("Thiếu ID tầng hợp lệ.");

                    Floor floor = floorRepository.findByIdAndBuilding_Hotel_Id(item.getFloorId(), hotelId)
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tầng hoặc tầng không thuộc khách sạn của tài khoản."));
                    String roomKey = item.getFloorId() + ":" + roomNumber;
                    if (!roomKeys.add(roomKey)
                            || roomRepository.existsByFloorIdAndRoomNumber(item.getFloorId(), roomNumber)) {
                        throw new IllegalArgumentException("Số phòng đã tồn tại ở tầng này.");
                    }

                    RoomType roomType = RoomType.valueOf(required(item.getRoomType(), "Thiếu loại phòng."));
                    RoomStatus roomStatus = RoomStatus.valueOf(required(item.getRoomStatus(), "Thiếu trạng thái phòng."));
                    if (item.getBeds() == null || item.getBeds().isEmpty()) {
                        throw new IllegalArgumentException("Phòng cần ít nhất một loại giường trong sheet Giường phòng.");
                    }
                    Set<Long> bedTypeIds = new HashSet<>();
                    for (var bed : item.getBeds()) {
                        if (bed == null || bed.getBedTypeId() == null || bed.getQuantity() == null || bed.getQuantity() < 1) {
                            throw new IllegalArgumentException("Mỗi loại giường cần ID hợp lệ và số lượng từ 1 trở lên.");
                        }
                        if (!bedTypeIds.add(bed.getBedTypeId())) {
                            throw new IllegalArgumentException("Không được khai báo trùng loại giường cho cùng một phòng.");
                        }
                    }
                    List<BedType> foundBedTypes = bedTypeRepository.findAllById(bedTypeIds);
                    if (foundBedTypes.size() != bedTypeIds.size()) {
                        throw new IllegalArgumentException("Có loại giường không tồn tại trong danh mục.");
                    }
                    foundBedTypes.forEach(bedType -> bedTypesById.put(bedType.getId(), bedType));
                    List<String> imageUrls = item.getImageUrls() == null ? List.of() : item.getImageUrls();
                    if (imageUrls.size() < 4 || imageUrls.size() > 8) {
                        throw new IllegalArgumentException("Mỗi phòng cần từ 4 đến 8 ảnh.");
                    }
                    if (imageUrls.stream().anyMatch(url -> !isCloudinaryImageUrl(url))) {
                        throw new IllegalArgumentException("URL ảnh phải là HTTPS URL hợp lệ từ Cloudinary.");
                    }

                    List<Long> amenityIds = item.getAmenityIds() == null ? List.of() : item.getAmenityIds();
                    List<Amenity> foundAmenities = amenityIds.isEmpty()
                            ? List.of()
                            : amenityRepository.findAllById(amenityIds);
                    if (foundAmenities.size() != new HashSet<>(amenityIds).size()) {
                        throw new IllegalArgumentException("Có tiện ích không tồn tại trong danh mục.");
                    }

                    BranchRoomPolicy policy = branchRoomPolicyRepository.findByHotelIdAndRoomType(hotelId, roomType);
                    Double basePrice = policy == null || policy.getBasePrice() == null ? 0.0 : policy.getBasePrice();
                    List<RoomImage> roomImages = new ArrayList<>();
                    for (int imageIndex = 0; imageIndex < imageUrls.size(); imageIndex++) {
                        roomImages.add(RoomImage.builder()
                                .url(imageUrls.get(imageIndex))
                                .isDefault(imageIndex == 0)
                                .build());
                    }

                    Room room = Room.builder()
                            .floor(floor)
                            .roomNumber(roomNumber)
                            .roomType(roomType)
                            .roomStatus(roomStatus)
                            .basePrice(basePrice)
                            .avatarUrl(roomImages)
                            .amenities(new HashSet<>(foundAmenities))
                            .build();

                    // ✅ CLONE DANH SÁCH GIƯỜNG ĐỘC LẬP CHO TỪNG PHÒNG ĐỂ TRÁNH BỊ CHIA SẺ THAM CHẾU
                    List<RoomBedRequest> clonedBeds = item.getBeds().stream()
                            .map(b -> RoomBedRequest.builder()
                                    .bedTypeId(b.getBedTypeId())
                                    .quantity(b.getQuantity())
                                    .build())
                            .toList();

                    roomsToSave.add(new RoomImportEntry(room, clonedBeds));
                    details.add(detail(rowNumber, roomNumber, "SUCCESS", "Đã kiểm tra."));
                } catch (RuntimeException error) {
                    details.add(detail(rowNumber, roomNumber, "FAILED", error.getMessage()));
                }

                int percent = 10 + (int) (((index + 1) / (double) total) * 75);
                taskStatusMap.put(taskId, new ImportTaskStatus(percent,
                        "Đang ánh xạ phòng và URL ảnh (" + (index + 1) + "/" + total + ").",
                        "PROCESSING", details));
                roomImportSocketEmitter.emitImportProgress(
                        hotelId,
                        taskId,
                        percent,
                        "Đang ánh xạ phòng và URL ảnh (" + (index + 1) + "/" + total + ").",
                        "PROCESSING",
                        details,
                        false);
            }

            if (!roomsToSave.isEmpty()) {
                new TransactionTemplate(transactionManager).executeWithoutResult(transactionStatus -> {
                    List<RoomBed> roomBedsToSave = new ArrayList<>();
                    for (RoomImportEntry entry : roomsToSave) {
                        Room savedRoom = roomRepository.save(entry.room());
                        for (RoomBedRequest bed : entry.beds()) {
                            roomBedsToSave.add(RoomBed.builder()
                                    .room(savedRoom)
                                    .bedType(bedTypesById.get(bed.getBedTypeId()))
                                    .quantity(bed.getQuantity())
                                    .build());
                        }
                    }
                    roomRepository.flush();
                    roomBedRepository.saveAllAndFlush(roomBedsToSave);
                    log.info("Saved {} imported rooms with {} room-specific bed entries",
                            roomsToSave.size(), roomBedsToSave.size());
                });
                roomSocketEmitter.emitRoomsImported(hotelId, roomsToSave.size());
            }
            long failedCount = details.stream().filter(row -> "FAILED".equals(row.get("result"))).count();
            String status = roomsToSave.isEmpty() ? "FAILED" : "SUCCESS";
            String message = roomsToSave.size() + "/" + total + " phòng đã được lưu."
                    + (failedCount > 0 ? " Có " + failedCount + " dòng không hợp lệ." : "");
            taskStatusMap.put(taskId, new ImportTaskStatus(100, message, status, details));
            roomImportSocketEmitter.emitImportProgress(
                    hotelId, taskId, 100, message, status, details, true);
        } catch (Exception error) {
            log.error("Room import task {} failed", taskId, error);
            String message = "Lỗi khi lưu dữ liệu phòng: " + error.getMessage();
            taskStatusMap.put(taskId, new ImportTaskStatus(0, message, "FAILED", details));
            roomImportSocketEmitter.emitImportProgress(
                    hotelId, taskId, 100, message, "FAILED", details, true);
        }
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private record RoomImportEntry(Room room, List<RoomBedRequest> beds) {

    }

    private static boolean isCloudinaryImageUrl(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "res.cloudinary.com".equalsIgnoreCase(uri.getHost())
                    && uri.getPath() != null
                    && uri.getPath().contains("/image/upload/");
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private static Map<String, Object> detail(int rowNumber, String roomNumber, String result, String message) {
        return Map.of(
                "rowNumber", rowNumber,
                "roomNumber", roomNumber,
                "result", result,
                "message", message == null || message.isBlank() ? result : message
        );
    }
}