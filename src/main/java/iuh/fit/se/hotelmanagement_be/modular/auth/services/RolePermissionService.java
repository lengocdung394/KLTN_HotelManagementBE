package iuh.fit.se.hotelmanagement_be.modular.auth.services;

import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CreateRoleRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ImportRolePermissionsRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.RolePermissionOverviewResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.RoleResponse;

import java.util.List;

public interface RolePermissionService {
    RoleResponse createRole(CreateRoleRequest request);
    List<RoleResponse> getRoles();
    List<RolePermissionOverviewResponse> importRolePermissions(
            ImportRolePermissionsRequest request);
    List<RolePermissionOverviewResponse> getRolePermissionOverview();
    void removePermissionFromRole(String roleCode, String permissionCode);
    void addPermissionFromRole(String roleCode, String permissionCode);
}
