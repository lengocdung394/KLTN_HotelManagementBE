package iuh.fit.se.hotelmanagement_be.modular.service.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.service.requests.CreateServiceRequest;
import iuh.fit.se.hotelmanagement_be.modular.service.requests.UpdateServiceRequest;
import iuh.fit.se.hotelmanagement_be.modular.service.responses.ServiceResponse;
import iuh.fit.se.hotelmanagement_be.modular.service.services.HotelServiceService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Hotel Services", description = "APIs quản lý và tra cứu danh mục dịch vụ khách sạn (Nhà hàng, Spa, Hội nghị, Đưa đón...)")
public class ServiceController {

    HotelServiceService hotelServiceService;

    @GetMapping
    @Operation(summary = "Lấy danh sách dịch vụ khách sạn", description = "Hỗ trợ lọc theo chi nhánh khách sạn, loại dịch vụ và trạng thái khả dụng.")
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getAllServices(
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean activeOnly) {

        List<ServiceResponse> result = hotelServiceService.getAllServices(hotelId, category, activeOnly);
        return ResponseEntity.ok(ApiResponse.<List<ServiceResponse>>builder()
                .code(200)
                .message("Lấy danh sách dịch vụ thành công")
                .result(result)
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một dịch vụ")
    public ResponseEntity<ApiResponse<ServiceResponse>> getServiceById(@PathVariable String id) {
        ServiceResponse result = hotelServiceService.getServiceById(id);
        return ResponseEntity.ok(ApiResponse.<ServiceResponse>builder()
                .code(200)
                .message("Lấy thông tin dịch vụ thành công")
                .result(result)
                .build());
    }

    @PostMapping
    @Operation(summary = "Tạo mới dịch vụ khách sạn (Dành cho Quản lý / Admin)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ServiceResponse>> createService(
            @Valid @RequestBody CreateServiceRequest request) {

        ServiceResponse result = hotelServiceService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<ServiceResponse>builder()
                .code(201)
                .message("Tạo dịch vụ thành công")
                .result(result)
                .build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật dịch vụ khách sạn")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ServiceResponse>> updateService(
            @PathVariable String id,
            @Valid @RequestBody UpdateServiceRequest request) {

        ServiceResponse result = hotelServiceService.updateService(id, request);
        return ResponseEntity.ok(ApiResponse.<ServiceResponse>builder()
                .code(200)
                .message("Cập nhật dịch vụ thành công")
                .result(result)
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Ẩn / Xóa dịch vụ khách sạn")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable String id) {
        hotelServiceService.deleteService(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Đã ẩn dịch vụ thành công")
                .build());
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Bật / Tắt trạng thái kinh doanh của dịch vụ")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ServiceResponse>> toggleStatus(@PathVariable String id) {
        ServiceResponse result = hotelServiceService.toggleServiceStatus(id);
        return ResponseEntity.ok(ApiResponse.<ServiceResponse>builder()
                .code(200)
                .message("Thay đổi trạng thái dịch vụ thành công")
                .result(result)
                .build());
    }
}
