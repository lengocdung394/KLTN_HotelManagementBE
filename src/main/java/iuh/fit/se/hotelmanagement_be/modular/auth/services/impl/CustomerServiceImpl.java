package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.CustomerRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.WalkInCustomerRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerFindByIdResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.CustomerService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CustomerServiceImpl implements CustomerService {
    CustomerRepository customerRepository;

    @Override
    public Customer createWalkInCustomer(WalkInCustomerRequest request) {
        // 1. Kiểm tra xem khách hàng đã tồn tại dựa vào SĐT hoặc CCCD chưa
        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_EXISTED);
        }
        if (customerRepository.existsByCccd(request.getCccd())) {
            throw new AppException(ErrorCode.CCCD_EXISTED);
        }

        // 2. Khởi tạo đối tượng Customer mới
        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setPhone(request.getPhone());
        customer.setCccd(request.getCccd());

        // Các trường khác như email, password, address... có thể để null hoặc giá trị mặc định cho khách walk-in

        // 3. Lưu vào Database (Mã ID dạng CUS_YYYYMMDD_XXXXXX sẽ tự động sinh nhờ @PrePersist)
        return customerRepository.save(customer);
    }

    @Override
    public CustomerFindByIdResponse getCustomerById(String id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
        return CustomerFindByIdResponse.builder()
                .name(customer.getFullName())
                .cccd(customer.getCccd())
                .phone(customer.getPhone()).build();
    }
}

