package iuh.fit.se.hotelmanagement_be.modular.auth.controllers;

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

    @PostMapping("/register-request")
    public ResponseEntity<String> registerRequest(@RequestBody UserRegisterRequest request) {
        authService.registerRequest(request);
        return ResponseEntity.ok("Mã OTP đã được gửi đến email của bạn!");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<UserResponse>> verifyOtp(@RequestBody VerifyOtpRequest request) {
        UserResponse response = authService.verifyOtpAndRegister(request);

        ApiResponse<UserResponse> apiResponse = ApiResponse.<UserResponse>builder()
                .code(200)
                .message("Xác thực OTP thành công! Tài khoản đã được kích hoạt.")
                .result(response)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    // API 2: Chỉ dùng để đăng nhập và lấy mã JWT Token
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
    // Gui lai ma OTP moi
    @PostMapping("/resend-otp")
    public ResponseEntity<String> resendOtp(@RequestBody ResendOtpRequest request) {
       otpService.resendOtp(request.getEmail());
       return  ResponseEntity.ok("Ma OTP moi gui den email cua ban");

    }

}
