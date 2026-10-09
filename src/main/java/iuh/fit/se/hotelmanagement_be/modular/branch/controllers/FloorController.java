package iuh.fit.se.hotelmanagement_be.modular.branch.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.branch.requests.FloorCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.branch.requests.FloorUpdateRequest;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.FloorResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.services.FloorService;
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
@RequestMapping("/floor")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Floor", description = "APIs liên quan đến quản lý tầng")
public class FloorController {
    FloorService floorService;

    @GetMapping("/getFloorsByBuildingId")
    @PreAuthorize("hasAuthority('VIEW_FLOORS')")
    @Operation(summary = "Lấy danh sách tầng theo ID tòa nhà")
    public ResponseEntity<ApiResponse<List<FloorResponse>>> getFloorsByBuildingId(
            @RequestParam String buildingId,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        List<FloorResponse> floors = floorService.getFloorsByBuildingId(buildingId);
        return ResponseEntity.ok(ApiResponse.<List<FloorResponse>>builder()
                .code(200)
                .result(floors)
                .build());
    }

    @GetMapping("/getFloorsByHotelId")
    @PreAuthorize("hasAuthority('VIEW_FLOORS')")
    @Operation(summary = "Lấy danh sách tầng thuộc khách sạn của tài khoản đăng nhập")
    public ResponseEntity<ApiResponse<List<FloorResponse>>> getFloorsByHotelId(Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        List<FloorResponse> floors = floorService.getAllFloors(account.getHotelId());
        return ResponseEntity.ok(ApiResponse.<List<FloorResponse>>builder()
                .code(200)
                .result(floors)
                .build());
    }

    @PostMapping("/createFloor")
    @PreAuthorize("hasAuthority('MANAGE_FLOORS') and principal.hotelId != null")
    @Operation(summary = "Tạo tầng trong tòa nhà thuộc khách sạn của tài khoản")
    public ResponseEntity<ApiResponse<FloorResponse>> createFloor(
            @Valid @RequestBody FloorCreateRequest request,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        FloorResponse floor = floorService.createFloor(request, account.getHotelId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<FloorResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Tạo tầng thành công.")
                .result(floor)
                .build());
    }

    @PutMapping("/updateFloor/{floorId}")
    @PreAuthorize("hasAuthority('MANAGE_FLOORS') and principal.hotelId != null")
    @Operation(summary = "Cập nhật thông tin tầng") // Thêm @Operation cho đồng bộ nếu cần
    public ResponseEntity<ApiResponse<FloorResponse>> updateFloor(
            @PathVariable String floorId,
            @Valid @RequestBody FloorUpdateRequest request,
            Authentication authentication
    ) {
        Account account = (Account) authentication.getPrincipal();
        FloorResponse floor = floorService.updateFloor(floorId, request, account.getHotelId());

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<FloorResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật tầng thành công.")
                .result(floor)
                .build());
    }

}
