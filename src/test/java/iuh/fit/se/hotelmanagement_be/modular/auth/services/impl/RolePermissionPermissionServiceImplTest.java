package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.PermissionRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.RoleRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CreateRoleRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolePermissionPermissionServiceImplTest {
    @Mock
    RoleRepository roleRepository;

    @Mock
    PermissionRepository permissionRepository;

    @Mock
    ThreadPoolTaskExecutorBuilder threadPoolTaskExecutorBuilder;

    @InjectMocks
    RolePermissionPermissionServiceImpl service;

    @Test
    void updateRoleChangesMetadata() {
        Role role = new Role();
        role.setCode("ROLE_SUPER_ADMIN");
        role.setSystem(true);
        when(roleRepository.findByCode("ROLE_SUPER_ADMIN")).thenReturn(Optional.of(role));
        when(roleRepository.save(role)).thenReturn(role);

        var result = service.updateRole("ROLE_SUPER_ADMIN", CreateRoleRequest.builder()
                .code("ROLE_SUPER_ADMIN")
                .name("Super Admin")
                .description("Updated description")
                .build());

        assertEquals("ROLE_SUPER_ADMIN", result.getCode());
        assertEquals("Super Admin", result.getName());
        assertEquals("Updated description", result.getDescription());
        verify(roleRepository).save(role);
    }

    @Test
    void updateRoleRejectsChangingSystemRoleCode() {
        Role role = new Role();
        role.setCode("ROLE_SUPER_ADMIN");
        role.setSystem(true);
        when(roleRepository.findByCode("ROLE_SUPER_ADMIN")).thenReturn(Optional.of(role));

        assertThrows(AppException.class, () -> service.updateRole("ROLE_SUPER_ADMIN",
                CreateRoleRequest.builder()
                        .code("ROLE_ADMIN_NEW")
                        .name("Super Admin")
                        .description("Updated description")
                        .build()));

        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void updateRoleChangesCodeForNonSystemRole() {
        Role role = new Role();
        role.setCode("ROLE_FRONT_DESK");
        when(roleRepository.findByCode("ROLE_FRONT_DESK")).thenReturn(Optional.of(role));
        when(roleRepository.existsByCode("ROLE_RECEPTIONIST")).thenReturn(false);
        when(roleRepository.save(role)).thenReturn(role);

        var result = service.updateRole("ROLE_FRONT_DESK", CreateRoleRequest.builder()
                .code("role_receptionist")
                .name("Receptionist")
                .description("Front desk role")
                .build());

        assertEquals("ROLE_RECEPTIONIST", result.getCode());
        assertEquals("ROLE_RECEPTIONIST", role.getCode());
        verify(roleRepository).save(role);
    }
}
