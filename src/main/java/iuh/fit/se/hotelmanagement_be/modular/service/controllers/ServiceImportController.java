package iuh.fit.se.hotelmanagement_be.modular.service.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.service.services.impl.ServiceImportService;
import iuh.fit.se.hotelmanagement_be.shared.entities.ImportTaskStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/servicesImport")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Hotel Services", description = "APIs quản lý và tra cứu danh mục dịch vụ khách sạn (Nhà hàng, Spa, Hội nghị, Đưa đón...)")
public class ServiceImportController {

    @Autowired
    private ServiceImportService serviceImportService;

    /**
     * 1. API nhận file ZIP và khởi chạy tiến trình import bất đồng bộ
     * Trả về taskId ngay lập tức để phía FE bắt đầu theo dõi tiến trình
     */
    @PostMapping("/import-zip-async")
    public ResponseEntity<?> startZipImport(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        Long hotelId = account.getHotelId();

        try {
            // Khởi chạy tiến trình ngầm và lấy mã taskId
            String taskId = serviceImportService.startAsyncImport(file, hotelId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "taskId", taskId,
                    "message", "Đã tiếp nhận file ZIP và bắt đầu xử lý ngầm."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 2. API để Frontend gọi định kỳ (polling) kiểm tra tiến trình dựa vào taskId
     */
    @GetMapping("/import-status/{taskId}")
    public ResponseEntity<ImportTaskStatus> getImportStatus(@PathVariable String taskId) {
        ImportTaskStatus status = serviceImportService.getTaskStatus(taskId);
        return ResponseEntity.ok(status);
    }
}
