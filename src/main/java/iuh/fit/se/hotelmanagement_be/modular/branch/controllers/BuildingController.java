package iuh.fit.se.hotelmanagement_be.modular.branch.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.branch.requests.BuildingCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.BuildingResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.services.BuildingService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/branch")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Building", description = "APIs liên quan đến quản ly toa nha")
public class BuildingController {
    BuildingService buildingService;

    @PreAuthorize("hasAuthority('VIEW_BUILDINGS')")
    @GetMapping("/getBuildingByHotelId")
    @Operation(
            summary = "Lấy danh sách tòa theo ID của khách sạn"

    )
    public ResponseEntity<ApiResponse<List<BuildingResponse>>> getBuildingsByHotelId(Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        Long hotelId = account.getHotelId();
        List<BuildingResponse> buildingResponseList = buildingService.getBuildingsByHotelId(hotelId);
        return ResponseEntity.ok(ApiResponse.<List<BuildingResponse>>builder()
                .code(100)
                .result(buildingResponseList)
                .message("Lay thanh cong danh sach")
                .build()

        );
    }

    @PostMapping("/createBuilding")
    @PreAuthorize("hasAuthority('MANAGE_BUILDINGS') and principal.hotelId != null")
    @Operation(summary = "Tạo tòa nhà trong khách sạn của tài khoản")
    public ResponseEntity<ApiResponse<BuildingResponse>> createBuilding(
            @Valid @RequestBody BuildingCreateRequest request,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        BuildingResponse building = buildingService.createBuilding(request, account.getHotelId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<BuildingResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tạo tòa nhà thành công.")
                .result(building)
                .build());
    }

    @PutMapping("/updateBuilding/{buildingId}")
    @PreAuthorize("hasAuthority('MANAGE_FLOORS') and principal.hotelId != null")
    @Operation(summary = "Cập nhật thông tin tầng") // Thêm @Operation cho đồng bộ nếu cần
    public ResponseEntity<ApiResponse<BuildingResponse>> updateFloor(
            @PathVariable String buildingId,
            @Valid @RequestBody BuildingCreateRequest request,
            Authentication authentication
    ) {
        Account account = (Account) authentication.getPrincipal();
        BuildingResponse building = buildingService.updateBuilding(buildingId, request, account.getHotelId());

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<BuildingResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật tầng thành công.")
                .result(building)
                .build());
    }


}
