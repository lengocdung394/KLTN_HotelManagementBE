package iuh.fit.se.hotelmanagement_be.modular.auth.controllers;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CreateRoleRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ImportPermissionCatalogRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ImportRolePermissionsRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.PermissionCatalogItemRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.PermissionCatalogResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.PermissionResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.RolePermissionOverviewResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.RoleResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.PermissionCatalogService;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.RolePermissionService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/role_permissions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RolePermissionController {

    RolePermissionService roleService;
    PermissionCatalogService permissionCatalogService;

    // lay tat ca role
    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_MANAGER', 'ROLE_ADMIN', 'ROLE_MANAGE', 'VIEW_ROLES', 'PERMISSION_MANAGE')")
    public ApiResponse<List<RoleResponse>> getRoles() {
        return new ApiResponse<>(
                200,
                "Success",
                roleService.getRoles()
        );
    }

    @GetMapping("/me/permissions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<String>> getMyPermissions(Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        Set<String> roleCodes = account.getRoles() == null
                ? Set.of()
                : account.getRoles().stream()
                .map(role -> role.getCode())
                .collect(Collectors.toSet());
        List<String> permissions = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> !roleCodes.contains(authority))
                .distinct()
                .sorted()
                .toList();
        return new ApiResponse<>(200, "Success", permissions);
    }

    // tao role
    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ApiResponse<RoleResponse> createRole(
            @Valid @RequestBody CreateRoleRequest request) {
        return new ApiResponse<>(
                201,
                "Role created",
                roleService.createRole(request)
        );
    }

    @PutMapping("/roles/{roleCode}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ApiResponse<RoleResponse> updateRole(
            @PathVariable String roleCode,
            @Valid @RequestBody CreateRoleRequest request) {
        return new ApiResponse<>(
                200,
                "Role updated",
                roleService.updateRole(roleCode, request)
        );
    }

    // lay tat ca permisison thuoc role
    @GetMapping("/roles/permissions")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_MANAGER', 'ROLE_ADMIN', 'PERMISSION_MANAGE', 'VIEW_PERMISSIONS')")
    public ApiResponse<List<RolePermissionOverviewResponse>> getRolePermissions() {
        return new ApiResponse<>(
                200,
                "Success",
                roleService.getRolePermissionOverview()
        );
    }

    // import bang file cho permisison vao role
    @PostMapping("/roles/permissions/import")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ApiResponse<List<RolePermissionOverviewResponse>> importRolePermissions(
            @Valid @RequestBody ImportRolePermissionsRequest request) {
        return new ApiResponse<>(
                200,
                "Permissions imported",
                roleService.importRolePermissions(request)
        );
    }

    // xoa permisison khoi role
    @DeleteMapping("/roles/{roleCode}/permissions/{permissionCode}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> removePermissionFromRole(
            @PathVariable String roleCode,
            @PathVariable String permissionCode) {
        roleService.removePermissionFromRole(roleCode, permissionCode);
        return ResponseEntity.noContent().build();
    }

    // them permisison vao role
    @PostMapping("/roles/{roleCode}/permissions/{permissionCode}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> addPermissionToRole(@PathVariable String roleCode, @PathVariable String permissionCode) {
        roleService.addPermissionFromRole(roleCode, permissionCode);
        return ResponseEntity.noContent().build();
    }

    // lay tat ca ds permission
    @GetMapping("/permissions")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_MANAGER', 'ROLE_ADMIN', 'PERMISSION_MANAGE', 'VIEW_PERMISSIONS')")
    public ApiResponse<List<PermissionCatalogResponse
            >> getPermissions() {
        return new ApiResponse<>(
                200,
                "Success",
                permissionCatalogService.getPermissions()
        );
    }

    // them le permission
    @PostMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ApiResponse<PermissionResponse> createPermission(
            @Valid @RequestBody PermissionCatalogItemRequest request
    ) {
        return new ApiResponse<>(
                201,
                "Permission created",
                permissionCatalogService.createPermission(request)
        );
    }

    @PutMapping("/permissions/{permissionCode}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ApiResponse<PermissionResponse> updatePermission(
            @PathVariable String permissionCode,
            @Valid @RequestBody PermissionCatalogItemRequest request
    ) {
        return new ApiResponse<>(
                200,
                "Permission updated",
                permissionCatalogService.updatePermission(permissionCode, request)
        );
    }

    @DeleteMapping("/permissions/{permissionCode}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> deletePermission(
            @PathVariable String permissionCode
    ) {
        permissionCatalogService.deletePermission(permissionCode);
        return ResponseEntity.noContent().build();
    }

    // import permission bang file
    @PostMapping("/permissions/import")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ApiResponse<List<PermissionCatalogResponse>> importPermissions(
            @Valid @RequestBody ImportPermissionCatalogRequest request
    ) {
        return new ApiResponse<>(
                200,
                "Permissions imported",
                permissionCatalogService.importPermissions(request)
        );
    }

}
