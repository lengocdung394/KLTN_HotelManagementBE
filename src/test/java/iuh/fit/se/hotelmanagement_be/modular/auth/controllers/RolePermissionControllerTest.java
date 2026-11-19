package iuh.fit.se.hotelmanagement_be.modular.auth.controllers;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Permission;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.PermissionCatalogService;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.RolePermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RolePermissionControllerTest {

    @Test
    void getMyPermissionsReturnsAssignedPermissionsWithoutRoleCodes() {
        Role role = Role.builder()
                .code("ROLE_STAFF")
                .permissions(Set.of(
                        Permission.builder().code("VIEW_REPORTS").build(),
                        Permission.builder().code("ROLE_MANAGE").build()
                ))
                .build();
        Account account = Account.builder().roles(Set.of(role)).build();
        var authentication = new UsernamePasswordAuthenticationToken(
                account,
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_STAFF"),
                        new SimpleGrantedAuthority("VIEW_REPORTS"),
                        new SimpleGrantedAuthority("ROLE_MANAGE")
                )
        );
        var controller = new RolePermissionController(null, null);

        var response = controller.getMyPermissions(authentication);

        assertEquals(List.of("ROLE_MANAGE", "VIEW_REPORTS"), response.getResult());
    }

    @Test
    void getMyPermissionsReturnsEmptyListWhenNoPermissionsAreAssigned() {
        Role role = Role.builder().code("ROLE_STAFF").permissions(Set.of()).build();
        Account account = Account.builder().roles(Set.of(role)).build();
        var authentication = new UsernamePasswordAuthenticationToken(
                account,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        );
        var controller = new RolePermissionController(null, null);

        var response = controller.getMyPermissions(authentication);

        assertEquals(List.of(), response.getResult());
    }
}
