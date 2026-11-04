package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.CustomerRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.OtpRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.RoleRepository;
import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerCheckRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CustomerRegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
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
    void rejectsWalkInWhenPhoneMatchesButCccdDoesNot() {
        Customer existingCustomer = Customer.builder()
                .id("CUS123")
                .fullName("Existing Guest")
                .phone("0901234567")
                .cccd("012345678901")
                .email("guest@example.com")
                .build();
        when(customerRepository.findByPhone("0901234567")).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.findByCccd("099999999999")).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> customerService.createWalkInCustomer(
                        iuh.fit.se.hotelmanagement_be.modular.auth.requests.WalkInCustomerRequest.builder()
                                .fullName("Typed Guest")
                                .phone("0901234567")
                                .cccd("099999999999")
                                .build(),
                        1L));

        assertEquals(ErrorCode.CUSTOMER_IDENTITY_MISMATCH, exception.getErrorCode());
        verify(customerRepository, never()).save(org.mockito.ArgumentMatchers.any(Customer.class));
    }

    @Test
    void registrationCheckReportsMismatchWhenOnlyPhoneMatches() {
        Customer existingCustomer = Customer.builder()
                .id("CUS123")
                .phone("0901234567")
                .cccd("012345678901")
                .build();
        when(customerRepository.findByPhone("0901234567")).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.findByCccd("099999999999")).thenReturn(Optional.empty());

        CustomerCheckRequest request = new CustomerCheckRequest();
        request.setPhone("0901234567");
        request.setCccd("099999999999");
        var response = customerService.checkCustomer(request);

        assertEquals("IDENTITY_MISMATCH", response.getStatus());
        assertEquals(ErrorCode.CUSTOMER_IDENTITY_MISMATCH.getMessage(), response.getMessage());
    }

    @Test
    void registrationCheckReportsMismatchWhenOnlyCccdMatches() {
        Customer existingCustomer = Customer.builder()
                .id("CUS123")
                .phone("0901234567")
                .cccd("012345678901")
                .build();
        when(customerRepository.findByPhone("0909999999")).thenReturn(Optional.empty());
        when(customerRepository.findByCccd("012345678901")).thenReturn(Optional.of(existingCustomer));

        CustomerCheckRequest request = new CustomerCheckRequest();
        request.setPhone("0909999999");
        request.setCccd("012345678901");
        var response = customerService.checkCustomer(request);

        assertEquals("IDENTITY_MISMATCH", response.getStatus());
        assertEquals(ErrorCode.CUSTOMER_IDENTITY_MISMATCH.getMessage(), response.getMessage());
    }

    @Test
    void registrationCheckAcceptsExactPairEvenIfSingleFieldLookupsReturnOtherRows() {
        Customer matchingCustomer = Customer.builder()
                .id("CUS_MATCH")
                .phone("0901234567")
                .cccd("012345678901")
                .build();
        Customer phoneDuplicate = Customer.builder()
                .id("CUS_PHONE")
                .phone("0901234567")
                .cccd("099999999999")
                .build();
        Customer cccdDuplicate = Customer.builder()
                .id("CUS_CCCD")
                .phone("0909999999")
                .cccd("012345678901")
                .build();

        when(customerRepository.findByPhoneAndCccd("0901234567", "012345678901"))
                .thenReturn(Optional.of(matchingCustomer));
        lenient().when(customerRepository.findByPhone("0901234567")).thenReturn(Optional.of(phoneDuplicate));
        lenient().when(customerRepository.findByCccd("012345678901")).thenReturn(Optional.of(cccdDuplicate));

        CustomerCheckRequest request = new CustomerCheckRequest();
        request.setPhone("0901234567");
        request.setCccd("012345678901");
        var response = customerService.checkCustomer(request);

        assertEquals("WALK_IN_CUSTOMER_NEEDS_ACCOUNT", response.getStatus());
    }

    @Test
    void registrationRequestRejectsMismatchBeforeSendingOtp() {
        Customer existingCustomer = Customer.builder()
                .id("CUS123")
                .phone("0901234567")
                .cccd("012345678901")
                .build();
        when(customerRepository.findByPhone("0901234567")).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.findByCccd("099999999999")).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> customerService.customerRegisterRequest(
                        CustomerRegisterRequest.builder()
                                .fullName("Existing Guest")
                                .phone("0901234567")
                                .cccd("099999999999")
                                .email("guest@example.com")
                                .password("password")
                                .build()));

        assertEquals(ErrorCode.CUSTOMER_IDENTITY_MISMATCH, exception.getErrorCode());
        verify(otpService, never()).generateOtpCode();
    }

    @Test
    void registrationRequestNormalizesEmailBeforeSavingOtp() {
        CustomerRegisterRequest request = CustomerRegisterRequest.builder()
                .fullName("New Guest")
                .phone("0901234567")
                .cccd("012345678901")
                .email(" Guest@Example.com ")
                .password("password")
                .build();
        when(customerRepository.findByPhoneAndCccd("0901234567", "012345678901"))
                .thenReturn(Optional.empty());
        when(customerRepository.findByPhone("0901234567")).thenReturn(Optional.empty());
        when(customerRepository.findByCccd("012345678901")).thenReturn(Optional.empty());
        when(customerRepository.findByEmail("guest@example.com")).thenReturn(Optional.empty());
        when(accountRepository.existsByEmail("guest@example.com")).thenReturn(false);
        when(otpRepository.existsPendingRegistrationByEmail(
                org.mockito.ArgumentMatchers.eq("guest@example.com"),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(false);
        when(otpService.generateOtpCode()).thenReturn("123456");

        customerService.customerRegisterRequest(request);

        assertEquals("guest@example.com", request.getEmail());
        verify(otpService).saveOtp("guest@example.com", "123456", request);
        verify(emailService).sendOtpEmail("guest@example.com", "123456");
    }
}
