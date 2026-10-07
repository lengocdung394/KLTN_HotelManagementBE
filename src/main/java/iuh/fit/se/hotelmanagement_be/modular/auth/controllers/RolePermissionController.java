package iuh.fit.se.hotelmanagement_be.modular.auth.controllers;

import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CreateRoleRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ImportRolePermissionsRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.RolePermissionOverviewResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.RoleResponse;
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

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ApiResponse<List<RoleResponse>> getRoles() {
        return new ApiResponse<>(
                200,
                "Success",
                roleService.getRoles()
        );
    }

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

    @GetMapping("/roles/permissions")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ApiResponse<List<RolePermissionOverviewResponse>> getRolePermissions() {
        return new ApiResponse<>(
                200,
                "Success",
                roleService.getRolePermissionOverview()
        );
    }

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

    @DeleteMapping("/roles/{roleCode}/permissions/{permissionCode}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> removePermissionFromRole(
            @PathVariable String roleCode,
            @PathVariable String permissionCode) {
        roleService.removePermissionFromRole(roleCode, permissionCode);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/roles/{roleCode}/permissions/{permissionCode}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> addPermissionToRole(@PathVariable String roleCode, @PathVariable String permissionCode) {
        roleService.addPermissionFromRole(roleCode, permissionCode);
        return ResponseEntity.noContent().build();
    }


}
