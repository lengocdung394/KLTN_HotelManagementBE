package iuh.fit.se.hotelmanagement_be.modular.auth.services;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserRegisterRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ChangePasswordRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UpdateProfileRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.EmployeeCreateResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.ProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    EmployeeCreateResponse createStaffAndAccount(UserRegisterRequest dto, MultipartFile avatarFile, Account currentAccount);

    ProfileResponse getMyProfile();

    ProfileResponse updateMyProfile(UpdateProfileRequest request);

    void changePassword(ChangePasswordRequest request);

}
