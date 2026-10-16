package iuh.fit.se.hotelmanagement_be.modular.auth.controllers;

import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerUpdateRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.SuperAdminAccountUpdateRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.SuperAdminAccountResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.SuperAdminAccountService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/super-admin/accounts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminAccountController {
    SuperAdminAccountService accountService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SuperAdminAccountResponse>>> getAccounts(
            @RequestParam(defaultValue = "ALL") String type) {
        return ResponseEntity.ok(response(accountService.getAccounts(type), "Lấy danh sách tài khoản thành công."));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<SuperAdminAccountResponse>> getAccountDetails(
            @PathVariable String accountId) {
        return ResponseEntity.ok(response(accountService.getAccountDetails(accountId), "Lấy chi tiết tài khoản thành công."));
    }

    @PutMapping("/{accountId}")
    public ResponseEntity<ApiResponse<SuperAdminAccountResponse>> updateAccount(
            @PathVariable String accountId,
            @Valid @RequestBody SuperAdminAccountUpdateRequest request) {
        return ResponseEntity.ok(response(accountService.updateAccount(accountId, request), "Cập nhật tài khoản thành công."));
    }

    @PostMapping("/{accountId}/password-reset")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@PathVariable String accountId) {
        accountService.requestPasswordReset(accountId);
        return ResponseEntity.ok(response(null, "Mã OTP đặt lại mật khẩu đã được gửi đến email của tài khoản."));
    }

    @GetMapping("/customers/{customerId}")
    public ResponseEntity<ApiResponse<SuperAdminAccountResponse>> getCustomerDetails(
            @PathVariable String customerId) {
        return ResponseEntity.ok(response(accountService.getCustomerDetails(customerId), "Lấy chi tiết hồ sơ khách hàng thành công."));
    }

    @PutMapping("/customers/{customerId}")
    public ResponseEntity<ApiResponse<SuperAdminAccountResponse>> updateCustomer(
            @PathVariable String customerId,
            @Valid @RequestBody CustomerUpdateRequest request) {
        return ResponseEntity.ok(response(accountService.updateCustomer(customerId, request), "Cập nhật hồ sơ khách hàng thành công."));
    }

    private <T> ApiResponse<T> response(T result, String message) {
        return ApiResponse.<T>builder().code(200).message(message).result(result).build();
    }

}
