package iuh.fit.se.hotelmanagement_be.modular.promotion.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.promotion.requests.ClaimPromotionRequest;
import iuh.fit.se.hotelmanagement_be.modular.promotion.responses.CustomerPromotionResponse;
import iuh.fit.se.hotelmanagement_be.modular.promotion.responses.PromotionGetListByCustomerResponse;
import iuh.fit.se.hotelmanagement_be.modular.promotion.services.CustomerPromotionService;
import iuh.fit.se.hotelmanagement_be.modular.promotion.services.PromotionService;
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
@RequestMapping("/customer-promotions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Customer Promotion Controller", description = "Quản lý các mã khuyến mãi mà khách hàng đã lưu/sở hữu")
public class CustomerPromotionController {
    PromotionService promotionService;
    CustomerPromotionService customerPromotionService;

    @GetMapping("/customer/{customerId}")
    @Operation(
            summary = "Lấy danh sách mã khuyến mãi đã lưu của khách hàng",
            description = "Truy vấn toàn bộ các mã giảm giá, voucher mà một khách hàng cụ thể đã lưu vào ví ưu đãi, kèm theo mô tả chi tiết, điều kiện áp dụng và thời hạn."
    )

    public ApiResponse<List<PromotionGetListByCustomerResponse>> getSavedPromotionsByCustomer(
            @Parameter(description = "ID của khách hàng cần tra cứu kho voucher", example = "1")
            @PathVariable Long customerId) {

        List<PromotionGetListByCustomerResponse> promotions = customerPromotionService.getPromotionsByCustomerId(customerId);
        return ApiResponse.<List<PromotionGetListByCustomerResponse>>builder()
                .result(promotions)
                .build();
    }



    @PostMapping("/claim")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Khách hàng lưu mã khuyến mãi vào Ví voucher")
    public ResponseEntity<ApiResponse<CustomerPromotionResponse>> claimPromotion(
            @Valid @RequestBody ClaimPromotionRequest request) {

        CustomerPromotionResponse result = customerPromotionService.claimPromotion(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CustomerPromotionResponse>builder()
                        .code(1000)
                        .message("Lưu mã khuyến mãi vào Ví thành công")
                        .result(result)
                        .build());
    }
}
