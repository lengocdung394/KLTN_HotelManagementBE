package iuh.fit.se.hotelmanagement_be.modular.auth.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ResendOtpRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserLoginRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserRegisterRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.VerifyOtpRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.AuthenticationResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.UserResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.AuthService;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.impl.OtpService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Authentication", description = "APIs liên quan đến Xác thực & Đăng ký")
public class AuthController {
    AuthService authService;
    OtpService otpService;

    @Operation(
            summary = "Yêu cầu đăng ký tài khoản khách hàng",
            description = "Nhận thông tin đăng ký từ khách hàng, kiểm tra trùng lặp và tự động tạo/gửi mã OTP qua email xác thực."
    )
    @PostMapping("/register-request")
    public ResponseEntity<ApiResponse<String>> customerRegisterRequest(@RequestBody UserRegisterRequest request) {
        authService.customerRegisterRequest(request);

        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .code(200)
                .message("Mã OTP đã được gửi đến email của bạn!")
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @Operation(
            summary = "Xác thực mã OTP đăng ký",
            description = "Kiểm tra mã OTP khách hàng nhập vào. Nếu hợp lệ, hệ thống sẽ kích hoạt tài khoản, gán mặc định vai trò ROLE_CUSTOMER và lưu chính thức vào cơ sở dữ liệu."
    )
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<UserResponse>> verifyOtpAndRegisterCustomer(@RequestBody VerifyOtpRequest request) {
        UserResponse response = authService.verifyOtpAndRegisterCustomer(request);

        ApiResponse<UserResponse> apiResponse = ApiResponse.<UserResponse>builder()
                .code(200)
                .message("Xác thực OTP thành công! Tài khoản đã được kích hoạt.")
                .result(response)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @Operation(
            summary = "Đăng nhập hệ thống",
            description = "Cổng đăng nhập chung cho cả Khách hàng, Nhân viên và Quản lý. Hệ thống sẽ xác thực email, mật khẩu và trả về chuỗi JWT Token chứa thông tin quyền hạn tương ứng."
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(@RequestBody UserLoginRequest request) {
        AuthenticationResponse response = authService.login(request);

        ApiResponse<AuthenticationResponse> apiResponse = ApiResponse.<AuthenticationResponse>builder()
                .code(200)
                .message("Đăng nhập thành công")
                .result(response)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @Operation(
            summary = "Gửi lại mã OTP mới",
            description = "Áp dụng khi khách hàng không nhận được mã hoặc mã cũ đã hết hạn. Hệ thống sẽ hủy mã cũ và sinh một mã OTP 6 số mới gửi về email."
    )
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOtp(@RequestBody ResendOtpRequest request) {
        otpService.resendOtp(request.getEmail());

        //
        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .code(200)
                .message("Mã OTP mới đã được gửi đến email của bạn.")
                .build();

        return ResponseEntity.ok(apiResponse);
    }

}
