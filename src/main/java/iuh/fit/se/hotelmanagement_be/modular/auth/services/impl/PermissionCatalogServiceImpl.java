package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Permission;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.PermissionRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ImportPermissionCatalogRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.PermissionCatalogItemRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.PermissionCatalogResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.PermissionCatalogService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class PermissionCatalogServiceImpl implements PermissionCatalogService {
    private static final Pattern PERMISSION_CODE_PATTERN =
            Pattern.compile("^[A-Z][A-Z0-9_.:-]*$");

    PermissionRepository permissionRepository;

    // lay danh sach permission
    @Override
    public List<PermissionCatalogResponse> getPermissions() {
        return permissionRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Permission::getCode))
                .map(this::toResponse)
                .toList();
    }

    // Tao phan quyen
    @Override
    public PermissionCatalogResponse createPermission(PermissionCatalogItemRequest request) {
        String code = normalizeCode(request.getCode());

        validateCode(code);

        if (permissionRepository.findByCode(code).isPresent()) {
            throw new AppException(ErrorCode.PERMISSION_EXIST);
        }

        Permission permission = Permission.builder()
                .code(code)
                .name(request.getName().trim())
                .description(normalizeDescription(request.getDescription()))
                .build();

        return toResponse(permissionRepository.save(permission));
    }

    // file excel nap vao he thong
    @Override
    public List<PermissionCatalogResponse> importPermissions(ImportPermissionCatalogRequest request) {
        Set<String> importedCodes = new HashSet<>();

        for (PermissionCatalogItemRequest item : request.getPermissions()) {
            String code = normalizeCode(item.getCode());
            validateCode(code);

            if (!importedCodes.add(code)) {
                throw new AppException(ErrorCode.PERMISSION_EXIST);
            }

            Permission permission = permissionRepository.findByCode(code)
                    .orElseGet(() -> Permission.builder()
                            .code(code)
                            .build());

            // Import theo mã: quyền mới được tạo, quyền cũ được cập nhật tên/mô tả.
            // Không chỉnh sửa role_permission nên không làm thay đổi quyền đã gán.
            permission.setName(item.getName().trim());
            permission.setDescription(
                    normalizeDescription(item.getDescription())
            );

            permissionRepository.save(permission);
        }

        return getPermissions();
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private void validateCode(String code) {
        if (!PERMISSION_CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "Mã quyền không hợp lệ: " + code
            );
        }
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    private PermissionCatalogResponse toResponse(Permission permission) {
        return PermissionCatalogResponse.builder()
                .code(permission.getCode())
                .name(permission.getName())
                .description(permission.getDescription())
                .build();
    }
}
