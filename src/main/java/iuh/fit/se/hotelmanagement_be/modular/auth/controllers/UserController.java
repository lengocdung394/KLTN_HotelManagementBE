package iuh.fit.se.hotelmanagement_be.modular.auth.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ChangePasswordRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UpdateProfileRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.ProfileResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.UserService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "User Profile", description = "APIs quan ly ho so ca nhan")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    UserService userService;

    @GetMapping("/me/profile")
    @Operation(summary = "Lay ho so ca nhan")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.<ProfileResponse>builder()
                .code(1000)
                .result(userService.getMyProfile())
                .message("Lay thong tin thanh cong")
                .build());
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Cap nhat ho so ca nhan")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.<ProfileResponse>builder()
                .code(1000)
                .result(userService.updateMyProfile(request))
                .message("Cap nhat ho so thanh cong")
                .build());
    }

    @PatchMapping("/me/change-password")
    @Operation(summary = "Doi mat khau")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(1000)
                .message("Doi mat khau thanh cong")
                .build());
    }
}