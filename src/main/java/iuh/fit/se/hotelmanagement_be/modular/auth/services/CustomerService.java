package iuh.fit.se.hotelmanagement_be.modular.auth.services;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerCheckRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerRegisterRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.VerifyOtpRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.WalkInCustomerRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerCheckResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerFindByIdResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerRegisterResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.UserResponse;

public interface CustomerService {

    Customer createWalkInCustomer(WalkInCustomerRequest request);

    CustomerFindByIdResponse getCustomerById(String id);

    // Bước 1: Kiểm tra email + lưu OTP tạm + gửi mail
    CustomerRegisterResponse customerRegisterRequest(CustomerRegisterRequest request);

    // Bước 2: Kiểm tra OTP + lưu User chính thức vào DB + trả về thông tin User
    UserResponse verifyOtpAndRegisterCustomer(
            VerifyOtpRequest request
    );
    CustomerCheckResponse checkCustomer(CustomerCheckRequest request);

}
