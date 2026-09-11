package iuh.fit.se.hotelmanagement_be.modular.auth.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.enums.LoyaltyTier;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Customer", description = "APIs liên quan đến Xác thực & Đăng ký")
public class CustomerController {

    @Operation(summary = "Lấy danh sách tất cả loại khách hàng (LoyaltyTier)")
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<LoyaltyTier>>> getAllLoyaltyTier() {
        List<LoyaltyTier> loyaltyTier = Arrays.asList(LoyaltyTier.values());
        return ResponseEntity.ok(ApiResponse.<List<LoyaltyTier>>builder()
                .code(1000)
                .message("Lay thanh cong danh sách loại khách hàng")
                .result(loyaltyTier)
                .build());
    }

    // Lay danh sach khach hàng

}
