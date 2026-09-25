package iuh.fit.se.hotelmanagement_be.modular.auth.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.enums.LoyaltyTier;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ChangePasswordRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerUpdateProfileRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerProfileResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.AuthService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping({"/customer", "/users"})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Customer", description = "APIs dành cho khách hàng: Hồ sơ, Loyalty, Đổi mật khẩu")
public class CustomerController {

    AuthService authService;

    @Operation(summary = "Lấy danh sách tất cả loại khách hàng (LoyaltyTier)")
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<LoyaltyTier>>> getAllLoyaltyTier() {
        List<LoyaltyTier> loyaltyTier = Arrays.asList(LoyaltyTier.values());
        return ResponseEntity.ok(ApiResponse.<List<LoyaltyTier>>builder()
                .code(1000)
                .message("Lấy thành công danh sách loại khách hàng")
                .result(loyaltyTier)
                .build());
    }

    @Operation(summary = "Lấy thông tin hồ sơ của khách hàng đang đăng nhập")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getMyProfile() {
        CustomerProfileResponse response = authService.getMyCustomerProfile();
        return ResponseEntity.ok(ApiResponse.<CustomerProfileResponse>builder()
                .code(1000)
                .message("Lấy thông tin hồ sơ thành công")
                .result(response)
                .build());
    }

    @Operation(summary = "Cập nhật hồ sơ cá nhân của khách hàng")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateMyProfile(
            @Valid @RequestBody CustomerUpdateProfileRequest request) {
        CustomerProfileResponse response = authService.updateMyCustomerProfile(request);
        return ResponseEntity.ok(ApiResponse.<CustomerProfileResponse>builder()
                .code(1000)
                .message("Cập nhật hồ sơ thành công")
                .result(response)
                .build());
    }

    @Operation(summary = "Đổi mật khẩu tài khoản khách hàng")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('CUSTOMER')")
    @PatchMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changeCustomerPassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(1000)
                .message("Đổi mật khẩu thành công")
                .build());
    }
}
