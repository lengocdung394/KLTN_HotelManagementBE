package iuh.fit.se.hotelmanagement_be.modular.auth.services;

import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ChangePasswordRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerUpdateProfileRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserLoginRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.VerifyOtpRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.AuthenticationResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerGetOneResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerProfileResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.UserResponse;

import java.util.List;

public interface AuthService {

    AuthenticationResponse login(UserLoginRequest request);

    // Hồ sơ khách hàng (Profile)
    CustomerProfileResponse getMyCustomerProfile();

    CustomerProfileResponse updateMyCustomerProfile(CustomerUpdateProfileRequest request);

    void changeCustomerPassword(ChangePasswordRequest request);

    void forgotPasswordRequest(iuh.fit.se.hotelmanagement_be.modular.auth.requests.ForgotPasswordRequest request);

    void resetPassword(iuh.fit.se.hotelmanagement_be.modular.auth.requests.ResetPasswordRequest request);

    List<CustomerGetOneResponse> getAllCustomers();

    List<CustomerGetOneResponse> getCustomersByHotelId(Long hotelId);
}