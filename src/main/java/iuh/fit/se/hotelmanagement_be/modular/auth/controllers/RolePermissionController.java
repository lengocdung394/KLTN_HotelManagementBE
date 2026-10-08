package iuh.fit.se.hotelmanagement_be.modular.auth.controllers;

import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CreateRoleRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ImportPermissionCatalogRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ImportRolePermissionsRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.PermissionCatalogItemRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.PermissionCatalogResponse;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/role_permissions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RolePermissionController {

    RolePermissionService roleService;
    PermissionCatalogService permissionCatalogService;

    // lay tat ca role
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ApiResponse<List<RoleResponse>> getRoles() {
        return new ApiResponse<>(
                200,
                "Success",
                roleService.getRoles()
        );
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

    // lay tat ca permisison thuoc role
    @GetMapping("/roles/permissions")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
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
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
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
    public ApiResponse<PermissionCatalogResponse> createPermission(
            @Valid @RequestBody PermissionCatalogItemRequest request
    ) {
        return new ApiResponse<>(
                201,
                "Permission created",
                permissionCatalogService.createPermission(request)
        );
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
