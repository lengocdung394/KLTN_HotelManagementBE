package iuh.fit.se.hotelmanagement_be.modular.service.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.service.repositories.ServiceRepository;
import iuh.fit.se.hotelmanagement_be.modular.service.requests.requestForExcel.ServiceExcelRequest;
import iuh.fit.se.hotelmanagement_be.shared.CloudinaryService;
import iuh.fit.se.hotelmanagement_be.shared.entities.CustomMultipartFile;
import iuh.fit.se.hotelmanagement_be.shared.entities.ImportTaskStatus;
import iuh.fit.se.hotelmanagement_be.shared.entities.ZipExtractionResult;
import iuh.fit.se.hotelmanagement_be.shared.enums.ImageCategory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ServiceImportService {
    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    // Quản lý trạng thái tiến trình (taskId -> TaskStatus)
    private final Map<String, ImportTaskStatus> taskStatusMap = new ConcurrentHashMap<>();

    public String startAsyncImport(MultipartFile zipFile, Long hotelId) {
        String taskId = UUID.randomUUID().toString();
        taskStatusMap.put(taskId, new ImportTaskStatus(0, "Đang khởi tạo...", "PROCESSING"));

        // Chạy ngầm bất đồng bộ
        CompletableFuture.runAsync(() -> processImportTask(taskId, zipFile, hotelId));
        return taskId;
    }

    public ImportTaskStatus getTaskStatus(String taskId) {
        return taskStatusMap.getOrDefault(taskId, new ImportTaskStatus(0, "Không tìm thấy tiến trình", "NOT_FOUND"));
    }

    @Async
    public void processImportTask(String taskId, MultipartFile zipFile, Long hotelId) {
        try {
            updateStatus(taskId, 5, "Đang giải nén và phân tích file ZIP...");

            // 💡 GỌI HÀM XỬ LÝ FILE ZIP DÙNG CHUNG Ở ĐÂY
            ZipExtractionResult extractedData = extractAndValidateZipFile(zipFile);

            Hotel hotel = null;
            if (hotelId != null) {
                hotel = hotelRepository.findById(hotelId).orElse(null);
            }
            String branchName = (hotel != null && hotel.getName() != null) ? hotel.getName() : "system";

            updateStatus(taskId, 20, "Đang đọc dữ liệu từ file Excel...");

            // Riêng phần đọc Excel này là đặc thù của chức năng "Import Dịch Vụ" nên để riêng ở đây
            List<ServiceExcelRequest> rawRows = parseExcelData(extractedData.getExcelInputStream());

            if (rawRows.isEmpty()) {
                completeTask(taskId, "Không có dịch vụ mới nào được thêm (trùng tên hoặc file trống).");
                return;
            }

            updateStatus(taskId, 30, "Đang tải ảnh lên Cloudinary song song (" + rawRows.size() + " mục)...");

            // Xử lý song song upload ảnh lên Cloudinary cho từng dịch vụ
            ExecutorService executor = Executors.newFixedThreadPool(8);
            Hotel finalHotel = hotel;
            int totalItems = rawRows.size();
            java.util.concurrent.atomic.AtomicInteger completedCount = new java.util.concurrent.atomic.AtomicInteger(0);

            Map<String, byte[]> imageFilesMap = extractedData.getImageFilesMap();

            List<? extends CompletableFuture<?>> futures = rawRows.stream().map(dto ->
                    CompletableFuture.supplyAsync(() -> {
                        String imageUrl = null;
                        if (dto.getImageFileName() != null && !dto.getImageFileName().isBlank() && imageFilesMap.containsKey(dto.getImageFileName())) {
                            byte[] imageBytes = imageFilesMap.get(dto.getImageFileName());
                            MultipartFile multipartFile = new CustomMultipartFile(imageBytes, dto.getImageFileName());
                            try {
                                if (finalHotel != null) {
                                    List<String> uploadedUrls = cloudinaryService.uploadBranchImages(List.of(multipartFile), branchName, ImageCategory.SERVICES);
                                    imageUrl = uploadedUrls.isEmpty() ? null : uploadedUrls.get(0);
                                } else {
                                    imageUrl = cloudinaryService.uploadImage(multipartFile, "system/services");
                                }
                            } catch (Exception ignored) {}
                        }

                        int currentDone = completedCount.incrementAndGet();
                        int currentPercent = 30 + (int) (((double) currentDone / totalItems) * 55);
                        updateStatus(taskId, currentPercent, "Đang xử lý dịch vụ: " + currentDone + "/" + totalItems);

                        return iuh.fit.se.hotelmanagement_be.modular.service.entities.Service.builder()
                                .name(dto.getName())
                                .description(dto.getDescription())
                                .price(dto.getPrice())
                                .unit(dto.getUnit() != null ? dto.getUnit() : "Lần")
                                .category(dto.getCategory() != null ? dto.getCategory() : "Khác")
                                .imageUrl(imageUrl)
                                .active(true)
                                .hotel(finalHotel)
                                .build();
                    }, executor)
            ).collect(Collectors.toList());

            List<iuh.fit.se.hotelmanagement_be.modular.service.entities.Service> servicesToSave = (List<iuh.fit.se.hotelmanagement_be.modular.service.entities.Service>) futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            executor.shutdown();

            updateStatus(taskId, 90, "Đang lưu vào cơ sở dữ liệu...");
            if (!servicesToSave.isEmpty()) {
                serviceRepository.saveAll(servicesToSave);
            }

            completeTask(taskId, "Import thành công " + servicesToSave.size() + " dịch vụ!");

        } catch (Exception e) {
            failTask(taskId, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    // ==========================================
    // 🛠️ HÀM TÁCH RIÊNG: XỬ LÝ VÀ VALIATE FILE ZIP (DÙNG CHUNG CHO CÁC TÍNH NĂNG KHÁC)
    // ==========================================
    public ZipExtractionResult extractAndValidateZipFile(MultipartFile zipFile) {
        if (zipFile == null || zipFile.isEmpty()) {
            throw new RuntimeException("Vui lòng tải lên một file ZIP hợp lệ!");
        }

        Map<String, byte[]> imageFilesMap = new HashMap<>();
        InputStream excelInputStream = null;
        long maxSizeBytes = 5 * 1024 * 1024; // Giới hạn 5MB cho mỗi ảnh

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName();

                // Bỏ qua rác hệ thống
                if (entry.isDirectory() || fileName.startsWith("__MACOSX") || fileName.contains("/.~") || fileName.startsWith(".")) {
                    zis.closeEntry();
                    continue;
                }

                byte[] bytes = zis.readAllBytes();

                // Nhận diện file Excel
                if ((fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) && !fileName.contains("~$")) {
                    excelInputStream = new java.io.ByteArrayInputStream(bytes);
                }
                // Nhận diện file ảnh trong thư mục images/ kèm kiểm tra dung lượng <= 5MB
                else if (fileName.contains("images/") && fileName.matches("(?i).*\\.(jpg|jpeg|png|webp)$")) {
                    if (bytes.length <= maxSizeBytes) {
                        String pureFileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                        if (!pureFileName.isBlank()) {
                            imageFilesMap.put(pureFileName, bytes);
                        }
                    }
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi giải nén file ZIP: " + e.getMessage());
        }

        if (excelInputStream == null) {
            throw new RuntimeException("Không tìm thấy file Excel hợp lệ bên trong file ZIP!");
        }

        return new ZipExtractionResult(excelInputStream, imageFilesMap);
    }

    // ==========================================
    // 🛠️ HÀM RIÊNG BIỆT: ĐỌC EXCEL DÀNH RIÊNG CHO DỊCH VỤ
    // ==========================================
    private List<ServiceExcelRequest> parseExcelData(InputStream excelInputStream) {
        List<ServiceExcelRequest> rawRows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(excelInputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getCellValueAsString(row.getCell(0));
                if (name == null || name.isBlank()) continue;

                String description = getCellValueAsString(row.getCell(1));
                String priceStr = getCellValueAsString(row.getCell(2));
                Double price = priceStr != null && !priceStr.isBlank() ? Double.parseDouble(priceStr) : 0.0;
                String unit = getCellValueAsString(row.getCell(3));
                String category = getCellValueAsString(row.getCell(4));
                String imageFileName = getCellValueAsString(row.getCell(5));

                if (serviceRepository.existsByNameIgnoreCase(name.trim())) {
                    continue;
                }

                rawRows.add(new ServiceExcelRequest(name.trim(), description, price, unit, category, imageFileName));
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc file Excel: " + e.getMessage());
        }
        return rawRows;
    }

    private void updateStatus(String taskId, int percent, String message) {
        taskStatusMap.put(taskId, new ImportTaskStatus(percent, message, "PROCESSING"));
    }

    private void completeTask(String taskId, String message) {
        taskStatusMap.put(taskId, new ImportTaskStatus(100, message, "SUCCESS"));
    }

    private void failTask(String taskId, String errorMsg) {
        taskStatusMap.put(taskId, new ImportTaskStatus(0, errorMsg, "FAILED"));
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return null;
        }
    }


}
