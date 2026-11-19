package iuh.fit.se.hotelmanagement_be.modular.service.controllers;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.service.requests.requestForExcel.ServiceExcelImportRequest;
import iuh.fit.se.hotelmanagement_be.modular.service.services.impl.ServiceImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceImportControllerTest {
    @Mock
    ServiceImportService serviceImportService;

    ServiceImportController controller;

    @BeforeEach
    void setUp() {
        controller = new ServiceImportController(serviceImportService);
    }

    @Test
    void superAdminImportUsesSelectedBranch() {
        ServiceExcelImportRequest request = new ServiceExcelImportRequest();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                Account.builder().build(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        );
        when(serviceImportService.startAsyncUrlImport(request, 42L)).thenReturn("task-id");

        controller.startUrlImport(request, 42L, authentication);

        verify(serviceImportService).startAsyncUrlImport(request, 42L);
    }

    @Test
    void branchAdminImportUsesAssignedBranch() {
        ServiceExcelImportRequest request = new ServiceExcelImportRequest();
        Account account = mock(Account.class);
        when(account.getHotelId()).thenReturn(7L);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                account,
                null,
                List.of(new SimpleGrantedAuthority("CREATE_SERVICE"))
        );
        when(serviceImportService.startAsyncUrlImport(request, 7L)).thenReturn("task-id");

        controller.startUrlImport(request, 42L, authentication);

        verify(serviceImportService).startAsyncUrlImport(request, 7L);
    }
}
