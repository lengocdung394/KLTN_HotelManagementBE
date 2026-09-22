package iuh.fit.se.hotelmanagement_be.modular.auth.services;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.WalkInCustomerRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerFindByIdResponse;

public interface CustomerService {

    Customer createWalkInCustomer(WalkInCustomerRequest request);
    CustomerFindByIdResponse getCustomerById(String id);
}
