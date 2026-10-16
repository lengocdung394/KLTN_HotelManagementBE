package iuh.fit.se.hotelmanagement_be.modular.auth.services;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerUpdateRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.SuperAdminAccountUpdateRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.SuperAdminAccountResponse;

import java.util.List;

public interface SuperAdminAccountService {
    List<SuperAdminAccountResponse> getAccounts(String requestedType);
    SuperAdminAccountResponse getAccountDetails(String id);
    SuperAdminAccountResponse updateAccount(String id, SuperAdminAccountUpdateRequest request);
    boolean matchesType(String accountType, String filter);
    void updateStaffRole(Account account, String requestedRole);
    SuperAdminAccountResponse getCustomerDetails(String customerId);
    SuperAdminAccountResponse updateCustomer(String customerId, CustomerUpdateRequest request);
    void requestPasswordReset(String accountId);
}
