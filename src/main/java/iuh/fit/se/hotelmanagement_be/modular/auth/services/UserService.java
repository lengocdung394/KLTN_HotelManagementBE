package iuh.fit.se.hotelmanagement_be.modular.auth.services;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.User;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserRegisterRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.UserCreateResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.UserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserCreateResponse createStaffAndAccount(UserRegisterRequest dto, MultipartFile avatarFile, Account currentAccount);

}
