package iuh.fit.se.hotelmanagement_be.modular.auth.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserRegisterRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.impl.UserServiceImpl;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/staffs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Staff Management", description = "APIs quản lý nhân sự và phân quyền")
@SecurityRequirement(name = "bearerAuth") //Giúp Swagger hiển thị khóa Bearer Token trên API này
public class StaffController {
    UserServiceImpl userService;

    @Operation(summary = "Tạo tài khoản nhân sự cấp dưới")
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
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
}
