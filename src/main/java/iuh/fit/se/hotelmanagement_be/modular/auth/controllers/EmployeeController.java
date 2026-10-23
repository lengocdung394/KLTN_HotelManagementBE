package iuh.fit.se.hotelmanagement_be.modular.auth.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserRegisterRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.EmployeeProfileUpdateRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.EmployeeResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.impl.EmployeeServiceImpl;
import iuh.fit.se.hotelmanagement_be.config.SecurityUtils;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.EmployeeResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Staff Management", description = "APIs quản lý nhân sự và phân quyền")
@SecurityRequirement(name = "bearerAuth") //Giúp Swagger hiển thị khóa Bearer Token trên API này
public class EmployeeController {
    EmployeeServiceImpl userService;

    @Operation(summary = "Cập nhật hồ sơ của nhân viên đang đăng nhập")
    @PutMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateMyProfile(
            @Valid @RequestBody EmployeeProfileUpdateRequest request,
            @AuthenticationPrincipal Account currentAccount
    ) {
        EmployeeResponse result = userService.updateMyProfile(request, currentAccount);
        return ResponseEntity.ok(ApiResponse.<EmployeeResponse>builder()
                .code(200)
                .message("Cập nhật thông tin tài khoản thành công!")
                .result(result)
                .build());
    }

    @Operation(summary = "Tạo tài khoản nhân sự cấp dưới")
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADD_STAFF')")
    public ResponseEntity<ApiResponse<Object>> createNewStaff(
            @Parameter(
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserRegisterRequest.class))
            )
            @RequestPart("staffInfo") UserRegisterRequest request,
            @RequestPart(value = "avatarUrl", required = false) MultipartFile avatarFile,
            @AuthenticationPrincipal Account currentAccount
    ){

        Object result = userService.createStaffAndAccount(request, avatarFile, currentAccount);

        ApiResponse<Object> apiResponse = ApiResponse.builder()
                .code(200)
                .message("Tạo tài khoản nhân sự thành công! Hệ thống đã cấp mật khẩu mặc định là 1111.")
                .result(result)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Lấy danh sách nhân viên theo chi nhánh khách sạn")
    @GetMapping("/hotel/{hotelId}")
    @PreAuthorize("hasAnyAuthority('VIEW_STAFFS', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getEmployeesByHotel(@PathVariable("hotelId") Long hotelId) {
        List<EmployeeResponse> result = userService.getEmployeesByHotelId(hotelId);
        return ResponseEntity.ok(ApiResponse.<List<EmployeeResponse>>builder()
                .code(200)
                .message("Lấy danh sách nhân viên thành công!")
                .result(result)
                .build());
    }

    @Operation(summary = "Lấy danh sách nhân viên của chi nhánh tài khoản đang đăng nhập")
    @GetMapping("/my-hotel")
    @PreAuthorize("hasAnyAuthority('VIEW_STAFFS', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getEmployeesForCurrentHotel() {
        Long hotelId = SecurityUtils.getCurrentUserHotelId();
        if (hotelId == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản hiện tại chưa được liên kết với chi nhánh khách sạn."
            );
        }
        List<EmployeeResponse> result = userService.getEmployeesByHotelId(hotelId);
        return ResponseEntity.ok(ApiResponse.<List<EmployeeResponse>>builder()
                .code(200)
                .message("Lấy danh sách nhân viên của chi nhánh thành công!")
                .result(result)
                .build());
    }

    @Operation(summary = "Lấy thông tin chi tiết một nhân viên")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_STAFFS')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeDetail(@PathVariable("id") String id) {
        EmployeeResponse result = userService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.<EmployeeResponse>builder()
                .code(200)
                .message("Lấy thông tin nhân viên thành công!")
                .result(result)
                .build());
    }

    @Operation(summary = "Cập nhật thông tin nhân sự")
    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable("id") String id,
            @Parameter(
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserRegisterRequest.class))
            )
            @RequestPart("staffInfo") UserRegisterRequest request,
            @RequestPart(value = "avatarUrl", required = false) MultipartFile avatarFile,
            @AuthenticationPrincipal Account currentAccount
    ) {
        EmployeeResponse result = userService.updateEmployee(id, request, avatarFile, currentAccount);
        return ResponseEntity.ok(ApiResponse.<EmployeeResponse>builder()
                .code(200)
                .message("Cập nhật thông tin nhân sự thành công!")
                .result(result)
                .build());
    }
}
