package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Permission;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.PermissionRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.RoleRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.CreateRoleRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ImportRolePermissionsRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.PermissionAssignmentRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.RolePermissionGroupRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.PermissionAssignmentResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.RolePermissionOverviewResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.RoleResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.RolePermissionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RolePermissionPermissionServiceImpl implements RolePermissionService {
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;

    @Override
    public RoleResponse createRole(CreateRoleRequest request) {
        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        String name = request.getName().trim();

        if (!code.matches("^ROLE_[A-Z0-9_]+$")) {
            throw new IllegalArgumentException(
                    "Mã role phải bắt đầu bằng ROLE_ và chỉ gồm chữ in hoa, số hoặc dấu gạch dưới"
            );
        }

        if (roleRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Role " + code + " đã tồn tại");
        }

        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        role.setSystem(false);

        Role saved = roleRepository.save(role);

        return new RoleResponse(
                saved.getCode(),
                saved.getName(),
                saved.getDescription(),
                saved.isSystem()
        );
    }

    @Override
    public List<RoleResponse> getRoles() {
        return roleRepository.findAll().stream()
                .map(role -> new RoleResponse(
                        role.getCode(),
                        role.getName(),
                        role.getDescription(),
                        role.isSystem()
                ))
                .toList();
    }

    @Override
    public List<RolePermissionOverviewResponse> importRolePermissions(ImportRolePermissionsRequest request) {
        Set<String> processedRoleCodes = new HashSet<>();

        for (RolePermissionGroupRequest group : request.getRolePermissions()) {
            String roleCode = group.getRoleCode().trim().toUpperCase(Locale.ROOT);

            if (!processedRoleCodes.add(roleCode)) {
                throw new IllegalArgumentException(
                        "Role " + roleCode + " xuất hiện nhiều lần trong request"
                );
            }

            Role role = roleRepository.findByCode(roleCode)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Không tìm thấy role " + roleCode
                    ));

            Set<String> processedPermissionCodes = new HashSet<>();
            Set<Permission> grantedPermissions = new HashSet<>();

            for (PermissionAssignmentRequest item : group.getPermissions()) {
                String permissionCode = item.getCode().trim().toUpperCase(Locale.ROOT);

                if (!permissionCode.matches("^[A-Z][A-Z0-9_.:-]*$")) {
                    throw new IllegalArgumentException(
                            "Mã quyền không hợp lệ: " + permissionCode
                    );
                }

                if (!processedPermissionCodes.add(permissionCode)) {
                    throw new IllegalArgumentException(
                            "Quyền " + permissionCode + " bị lặp trong role " + roleCode
                    );
                }

                Permission permission = permissionRepository.findByCode(permissionCode)
                        .orElseGet(() -> {
                            Permission newPermission = new Permission();
                            newPermission.setCode(permissionCode);
                            return newPermission;
                        });

                permission.setName(item.getName().trim());
                permission.setDescription(
                        item.getDescription() == null ? "" : item.getDescription().trim()
                );

                Permission savedPermission = permissionRepository.save(permission);

                if (item.isGranted()) {
                    grantedPermissions.add(savedPermission);
                }
            }

            // Đây là cấu hình đầy đủ của role trong file: quyền được đánh "Không"
            // sẽ không còn được gán cho role đó.
            role.setPermissions(grantedPermissions);
            roleRepository.save(role);
        }
        return getRolePermissionOverview();
    }

    @Override
    public List<RolePermissionOverviewResponse> getRolePermissionOverview() {
        List<Permission> allPermissions = permissionRepository.findAll();

        return roleRepository.findAll().stream()
                .map(role -> {
                    Set<String> assignedCodes = role.getPermissions().stream()
                            .map(Permission::getCode)
                            .collect(Collectors
                                    .toSet());

                    List<PermissionAssignmentResponse> permissions = allPermissions.stream()
                            .map(permission -> new PermissionAssignmentResponse(
                                    permission.getCode(),
                                    permission.getName(),
                                    permission.getDescription(),
                                    assignedCodes.contains(permission.getCode())
                            ))
                            .toList();

                    return new RolePermissionOverviewResponse(
                            role.getCode(),
                            permissions
                    );
                })
                .toList();
    }

    @Override
    public void removePermissionFromRole(String roleCode, String permissionCode) {
        String normalizedRoleCode = roleCode.trim().toUpperCase(Locale.ROOT);
        String normalizedPermissionCode = permissionCode.trim().toUpperCase(Locale.ROOT);

        // tim role va permission
        Role role = roleRepository.findByCode(normalizedRoleCode).orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        Permission permission = permissionRepository.findByCode(normalizedPermissionCode).orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        // TIM KIEM VA XOA
        boolean removed = role.getPermissions().removeIf(permission::equals);
        if (removed) {
            roleRepository.save(role);
        }
    }

    @Override
    public void addPermissionFromRole(String roleCode, String permissionCode) {
        String normalizedRoleCode = roleCode.trim().toUpperCase(Locale.ROOT);
        String normalizedPermissionCode = permissionCode.trim().toUpperCase(Locale.ROOT);

        // tim role va permission
        Role role = roleRepository.findByCode(normalizedRoleCode).orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        Permission permission = permissionRepository.findByCode(normalizedPermissionCode).orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        // TIM KIEM VA XOA
        boolean add = role.getPermissions().add(permission);
        if (add) {
            roleRepository.save(role);
        }
    }
}
