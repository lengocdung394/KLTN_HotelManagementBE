package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.CustomerRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.OtpRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.RoleRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.CustomerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {
    @Mock CustomerRepository customerRepository;
    @Mock AccountRepository accountRepository;
    @Mock OtpRepository otpRepository;
    @Mock OtpService otpService;
    @Mock EmailService emailService;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CustomerSocketEmitter customerSocketEmitter;
    @Mock AccountSocketEmitter accountSocketEmitter;

    private CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(
                customerRepository,
                accountRepository,
                otpRepository,
                otpService,
                emailService,
                roleRepository,
                passwordEncoder,
                customerSocketEmitter,
                accountSocketEmitter);
    }

    @Test
    void returnsExistingCustomerWhenPhoneMatchesSoBookingCanContinue() {
        Customer existingCustomer = Customer.builder()
                .id("CUS123")
                .fullName("Existing Guest")
                .phone("0901234567")
                .cccd("012345678901")
                .email("guest@example.com")
                .build();
        when(customerRepository.findByPhone("0901234567")).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.findByCccd("099999999999")).thenReturn(Optional.empty());

        CustomerResponse response = customerService.createWalkInCustomer(
                iuh.fit.se.hotelmanagement_be.modular.auth.requests.WalkInCustomerRequest.builder()
                        .fullName("Typed Guest")
                        .phone("0901234567")
                        .cccd("099999999999")
                        .build(),
                1L);

        assertEquals("CUS123", response.getId());
        assertEquals("Existing Guest", response.getFullName());
        assertEquals("012345678901", response.getCccd());
        assertTrue(response.isAlreadyExists());
        verify(customerRepository, never()).save(org.mockito.ArgumentMatchers.any(Customer.class));
    }
}
