package iuh.fit.se.hotelmanagement_be.modular.payment.controllers;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.payment.requests.CheckoutRefundReviewRequest;
import iuh.fit.se.hotelmanagement_be.modular.payment.responses.CheckoutRefundResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.services.impl.CheckoutRefundService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/management-bookings/checkout-refunds")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasAuthority('UPDATE_BOOKING') or hasRole('SUPER_ADMIN')")
public class CheckoutRefundManagementController {
    CheckoutRefundService checkoutRefundService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CheckoutRefundResponse>>> getRefunds(Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<List<CheckoutRefundResponse>>builder()
                .code(1000)
                .result(checkoutRefundService.getRefunds(account))
                .build());
    }

    @PatchMapping("/{refundId}/bank-details")
    public ResponseEntity<ApiResponse<CheckoutRefundResponse>> updateBankDetails(
            @PathVariable String refundId,
            @Valid @RequestBody CheckoutRefundReviewRequest request,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<CheckoutRefundResponse>builder()
                .code(1000)
                .message("Đã cập nhật tài khoản nhận hoàn.")
                .result(checkoutRefundService.updateBankDetails(refundId, request, account))
                .build());
    }

    @PatchMapping("/{refundId}/approve")
    public ResponseEntity<ApiResponse<CheckoutRefundResponse>> approve(
            @PathVariable String refundId,
            @Valid @RequestBody(required = false) CheckoutRefundReviewRequest request,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<CheckoutRefundResponse>builder()
                .code(1000)
                .message("Đã duyệt hoàn dư check-out; thực hiện chuyển khoản thủ công trong 1–3 ngày.")
                .result(checkoutRefundService.approve(refundId, request, account))
                .build());
    }

    @PatchMapping("/{refundId}/confirm-refund")
    public ResponseEntity<ApiResponse<CheckoutRefundResponse>> confirmRefund(
            @PathVariable String refundId,
            @Valid @RequestBody(required = false) CheckoutRefundReviewRequest request,
            Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<CheckoutRefundResponse>builder()
                .code(1000)
                .message("Đã ghi nhận chuyển khoản hoàn dư check-out.")
                .result(checkoutRefundService.confirmRefund(refundId, request, account))
                .build());
    }
}
