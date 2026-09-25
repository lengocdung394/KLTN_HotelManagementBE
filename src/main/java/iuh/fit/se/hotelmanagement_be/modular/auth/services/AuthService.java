package iuh.fit.se.hotelmanagement_be.modular.auth.services;

import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ChangePasswordRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerUpdateProfileRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserLoginRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.VerifyOtpRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.AuthenticationResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerProfileResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.UserResponse;

public interface AuthService {
    // Bước 1: Kiểm tra email + lưu OTP tạm + gửi mail
    void customerRegisterRequest(CustomerCreateRequest request);

    // Bước 2: Kiểm tra OTP + lưu User chính thức vào DB + trả về thông tin User
    UserResponse verifyOtpAndRegisterCustomer(VerifyOtpRequest request);

    AuthenticationResponse login(UserLoginRequest request);

    // Hồ sơ khách hàng (Profile)
    CustomerProfileResponse getMyCustomerProfile();

    CustomerProfileResponse updateMyCustomerProfile(CustomerUpdateProfileRequest request);

    void changeCustomerPassword(ChangePasswordRequest request);

    void forgotPasswordRequest(iuh.fit.se.hotelmanagement_be.modular.auth.requests.ForgotPasswordRequest request);

    void resetPassword(iuh.fit.se.hotelmanagement_be.modular.auth.requests.ResetPasswordRequest request);
}