package iuh.fit.se.hotelmanagement_be.modular.room.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.BedTypeRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.BedTypeGetAllResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.services.BedTypeService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bedTypes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "BedType", description = "APIs liên quan đến quản lý tiện nghi")
public class BedTypeController {

    BedTypeService bedTypeService;

    @Operation(summary = "Lay danh sach trang thai phong (RoomStatus)")
    @GetMapping("/getAll")
    public ResponseEntity<ApiResponse<List<BedTypeGetAllResponse>>> getAllBedTypes() {

        List<BedTypeGetAllResponse> bedTypeServiceAll = bedTypeService.findAll();

        return ResponseEntity.ok(ApiResponse.<List<BedTypeGetAllResponse>>builder()
                .code(200)
                .message("Lay ds trang thai phong thanh cong")
                .result(bedTypeServiceAll)
                .build()
        );
    }

    @PostMapping("/createBedType")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BedTypeGetAllResponse>> createBedType(
            @Valid @RequestBody BedTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<BedTypeGetAllResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Thêm loại giường thành công.")
                .result(bedTypeService.create(request))
                .build());
    }

    @PutMapping("/updateBedType/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BedTypeGetAllResponse>> updateBedType(
            @PathVariable Long id,
            @Valid @RequestBody BedTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.<BedTypeGetAllResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật loại giường thành công.")
                .result(bedTypeService.update(id, request))
                .build());
    }

    @PostMapping("/importExcel")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<BedTypeGetAllResponse>>> importBedTypes(
            @Valid @RequestBody List<@Valid BedTypeRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<List<BedTypeGetAllResponse>>builder()
                .code(HttpStatus.CREATED.value())
                .message("Nhập danh sách loại giường thành công.")
                .result(bedTypeService.importAll(requests))
                .build());
    }

}
