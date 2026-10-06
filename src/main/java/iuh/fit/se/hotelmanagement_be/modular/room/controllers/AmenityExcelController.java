package iuh.fit.se.hotelmanagement_be.modular.room.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.requestForAmenityExcel.AmenityExcelImportRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.services.AmenityExcelService;
import iuh.fit.se.hotelmanagement_be.shared.entities.ImportTaskStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/amenitiesExcel")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Amenity", description = "APIs liên quan đến quản lý upload file")
public class AmenityExcelController {

    AmenityExcelService amenityExcelService;

    @PostMapping("/importExcel/async")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @Operation(summary = "Bắt đầu nhập tiện nghi và trả về mã tiến trình")
    public ResponseEntity<Map<String, String>> startAsyncImport(
            @RequestBody AmenityExcelImportRequest request) {
        String taskId = amenityExcelService.startAsyncImport(request);
        return ResponseEntity.accepted().body(Map.of(
                "taskId", taskId,
                "message", "Đã nhận dữ liệu tiện nghi và bắt đầu xử lý."
        ));
    }

    @GetMapping("/import-status/{taskId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @Operation(summary = "Lấy tiến trình nhập tiện nghi")
    public ResponseEntity<ImportTaskStatus> getImportStatus(@PathVariable String taskId) {
        return ResponseEntity.ok(amenityExcelService.getImportStatus(taskId));
    }
}
