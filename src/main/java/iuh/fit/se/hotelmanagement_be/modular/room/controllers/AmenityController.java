package iuh.fit.se.hotelmanagement_be.modular.room.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.AmenityRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.requestForAmenityExcel.AmenityExcelImportRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.AmenityGetAllResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.services.AmenityExcelService;
import iuh.fit.se.hotelmanagement_be.modular.room.services.AmenityService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/amenities")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Amenity", description = "APIs liên quan đến quản lý tiện nghi")
public class AmenityController {
    AmenityService amenityService;
    AmenityExcelService amenityExcelService;

    @GetMapping("/getAll")
    @Operation(summary = "Lấy danh sách tất cả tiện ích")
    public ResponseEntity<ApiResponse<List<AmenityGetAllResponse>>> getAll() {
        List<AmenityGetAllResponse> amenities = amenityService.getAllAmenities();

        return ResponseEntity.ok(ApiResponse.<List<AmenityGetAllResponse>>builder()
                .code(200)
                .message("Lấy danh sách tiện ích thành công!")
                .result(amenities)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AmenityGetAllResponse>> create(
            @Valid @RequestBody AmenityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<AmenityGetAllResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Thêm tiện ích thành công.")
                .result(amenityService.createAmenity(request))
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AmenityGetAllResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody AmenityRequest request) {
        return ResponseEntity.ok(ApiResponse.<AmenityGetAllResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật tiện ích thành công.")
                .result(amenityService.updateAmenity(id, request))
                .build());
    }

    @PostMapping("/importExcel")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<AmenityGetAllResponse>>> importAmenities(
            @Valid @RequestBody AmenityExcelImportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<List<AmenityGetAllResponse>>builder()
                .code(HttpStatus.CREATED.value())
                .message("Nhập tiện ích thành công.")
                .result(amenityExcelService.importAmenities(request))
                .build());
    }
}
