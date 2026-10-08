package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Permission;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.PermissionRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.ImportPermissionCatalogRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.PermissionCatalogItemRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.PermissionCatalogResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.responses.PermissionResponse;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.PermissionCatalogService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        // 1. Lấy toàn bộ permission từ DB và sắp xếp (nếu cần)
        List<Permission> permissions = permissionRepository.findAll();

        //2. Gom nhom theo truong category
        Map<String, List<Permission>> permissionsMap = permissions.stream().collect(Collectors.groupingBy(Permission::getCategory));

        // builder
        return permissionsMap.entrySet().stream().map(
                        entry -> {
                            String category = entry.getKey();
                            List<Permission> permsInCategory = entry.getValue();
                            // Sắp xếp các quyền bên trong danh mục theo code (nếu muốn)
                            permsInCategory.sort(Comparator.comparing(Permission::getCode));

                            // Map sang list response của từng permission con
                            List<PermissionResponse> permissionResponses = permsInCategory.stream()
                                    .map(this::toResponseFor) // hàm map từng permission lẻ cũ của bạn
                                    .toList();

                            return PermissionCatalogResponse.builder()
                                    .category(category)
                                    .permissions(permissionResponses)
                                    .build();

                        }
                )// Sắp xếp các nhóm danh mục theo tên category cho đẹp (tùy chọn)
                .sorted(Comparator.comparing(PermissionCatalogResponse::getCategory))
                .toList();


    }

    // Tao phan quyen
    @Override
    public PermissionResponse createPermission(PermissionCatalogItemRequest request) {
        String code = normalizeCode(request.getCode());

        validateCode(code, request.getCategory());

        if (permissionRepository.findByCode(code).isPresent()) {
            throw new AppException(ErrorCode.PERMISSION_EXIST);
        }


        Permission permission = Permission.builder()
                .code(code)
                .name(request.getName().trim())
                .description(normalizeDescription(request.getDescription()))
                .build();

        return toResponseFor(permissionRepository.save(permission));
    }

    // file excel nap vao he thong
    @Override
    public List<PermissionCatalogResponse> importPermissions(ImportPermissionCatalogRequest request) {
        Set<String> importedCodes = new HashSet<>();

        for (PermissionCatalogItemRequest item : request.getPermissions()) {
            String code = normalizeCode(item.getCode());
            validateCode(code, item.getCategory());

            if (!importedCodes.add(code)) {
                throw new AppException(ErrorCode.PERMISSION_EXIST);
            }


            // kiem tra xem da co category chua
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

    private void validateCode(String code, String category) {
        if (!PERMISSION_CODE_PATTERN.matcher(code).matches()) {
            throw new AppException(ErrorCode.PERMISSION_CODE_NOTVALID);
        }

        if (category == null || !category.isEmpty()) {
            throw new AppException(ErrorCode.PERMISSION_NOT_CATEGORY);

        }
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }


    private PermissionResponse toResponseFor(Permission permission) {
        return PermissionResponse.builder()
                .code(permission.getCode())
                .name(permission.getName())
                .description(permission.getDescription())
                .category(permission.getCategory())
                .build();
    }
}
