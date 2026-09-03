package iuh.fit.se.hotelmanagement_be.modular.auth.services;

import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserLoginRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserRegisterRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.VerifyOtpRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.AuthenticationResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.UserResponse;

public interface AuthService {
    // Bước 1: Kiểm tra email + lưu OTP tạm + gửi mail
    void customerRegisterRequest(UserRegisterRequest request);

    // Bước 2: Kiểm tra OTP + lưu User chính thức vào DB + trả về thông tin User
    UserResponse verifyOtpAndRegisterCustomer(VerifyOtpRequest request);

    AuthenticationResponse login (UserLoginRequest request);



}