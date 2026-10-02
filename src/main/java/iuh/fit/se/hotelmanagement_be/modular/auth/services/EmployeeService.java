package iuh.fit.se.hotelmanagement_be.modular.auth.services;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserRegisterRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.VerifyOtpRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.EmployeeCreateResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.UserResponse;
import org.springframework.web.multipart.MultipartFile;

import iuh.fit.se.hotelmanagement_be.modular.auth.responses.EmployeeResponse;
import java.util.List;

public interface EmployeeService {

    EmployeeCreateResponse createStaffAndAccount(UserRegisterRequest dto, MultipartFile avatarFile, Account currentAccount);

    List<EmployeeResponse> getEmployeesByHotelId(Long hotelId);

    EmployeeResponse getEmployeeById(String id);

    EmployeeResponse updateEmployee(String id, UserRegisterRequest dto, MultipartFile avatarFile);
}
