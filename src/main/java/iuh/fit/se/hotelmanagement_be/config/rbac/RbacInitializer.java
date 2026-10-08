package iuh.fit.se.hotelmanagement_be.config.rbac;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Permission;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Role;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.PermissionRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Order(1)
public class RbacInitializer implements CommandLineRunner {

    private static final String SUPER_ADMIN_ROLE = "ROLE_SUPER_ADMIN";

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RbacConfig rbacConfig;
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${BOOTSTRAP_SUPER_ADMIN_EMAIL:}")
    private String superAdminEmail;

    @Value("${BOOTSTRAP_SUPER_ADMIN_PASSWORD:}")
    private String superAdminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        Set<Permission> bootstrapPermissions = ensureBootstrapPermissions();
        Role superAdminRole = ensureSuperAdminRole(bootstrapPermissions);
        ensureSuperAdminAccount(superAdminRole);
    }

    /**
     * Tạo các permission nền tảng nếu chưa có.
     * Những permission này phải có sẵn để Super Admin sử dụng chức năng quản trị.
     */
    private Set<Permission> ensureBootstrapPermissions() {
        if (rbacConfig.getPermissions() == null || rbacConfig.getPermissions().isEmpty()) {
            throw new IllegalStateException(
                    "Chưa cấu hình permission nền tảng cho Super Admin"
            );
        }

        Set<Permission> permissions = new HashSet<>();

        for (RbacConfig.PermissionConfig configured : rbacConfig.getPermissions()) {
            if (configured == null
                    || configured.getCode() == null
                    || configured.getCode().isBlank()) {
                throw new AppException(ErrorCode.PERMISSION_NOT_CODE);
            }

            if (configured.getName() == null || configured.getName().isBlank()) {
                throw new AppException(ErrorCode.PERMISSION_NOT_NAME);
            }

            String code = configured.getCode()
                    .trim()
                    .toUpperCase(Locale.ROOT);

            String name = configured.getName().trim();

            Permission permission = permissionRepository.findByCode(code)
                    .orElseGet(() -> Permission.builder()
                            .code(code)
                            .build()
                    );

            permission.setName(name);
            permission.setDescription(
                    configured.getDescription() == null
                            ? ""
                            : configured.getDescription().trim()
            );
            if (configured.getCategory() == null || configured.getCategory().isBlank()) {
                throw new AppException(ErrorCode.PERMISSION_NOT_CATEGORY);
            }

            permission.setCategory(configured.getCategory().trim());

            permissions.add(permissionRepository.save(permission));
        }

        if (permissions.isEmpty()) {
            throw new AppException(ErrorCode.ROLE_NOT_PERMISSION);
        }

        return permissions;
    }

    /**
     * Tạo ROLE_SUPER_ADMIN nếu chưa có và thêm permission nền tảng vào role.
     * Không xóa các permission khác đã được gán cho role này.
     */
    private Role ensureSuperAdminRole(Set<Permission> bootstrapPermissions) {
        // Sửa lại: Tìm theo code thay vì name để đảm bảo chính xác tuyệt đối
        Role role = roleRepository.findByCode(SUPER_ADMIN_ROLE)
                .orElseGet(() -> Role.builder()
                        .name("Super Admin") // Tên hiển thị
                        .code(SUPER_ADMIN_ROLE)
                        .permissions(new HashSet<>())
                        .build()
                );

        // Đảm bảo khởi tạo Set nếu đang null
        if (role.getPermissions() == null) {
            role.setPermissions(new HashSet<>());
        }

        // Thêm các quyền mới vào
        role.getPermissions().addAll(bootstrapPermissions);

        // Lưu và flush thẳng xuống DB để cập nhật bảng trung gian ngay lập tức
        return roleRepository.saveAndFlush(role);
    }
    /**
     * Tạo tài khoản Super Admin nếu chưa có.
     * Nếu đã tồn tại thì giữ mật khẩu hiện tại và chỉ bổ sung role nếu cần.
     */
    private void ensureSuperAdminAccount(Role superAdminRole) {
        if (superAdminEmail == null || superAdminEmail.isBlank()) {
            throw new IllegalStateException(
                    "Thiếu cấu hình BOOTSTRAP_SUPER_ADMIN_EMAIL"
            );
        }

        if (superAdminPassword == null || superAdminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Thiếu cấu hình BOOTSTRAP_SUPER_ADMIN_PASSWORD"
            );
        }

        String email = superAdminEmail
                .trim()
                .toLowerCase(Locale.ROOT);

        Account account = accountRepository.findByEmail(email).orElse(null);

        if (account == null) {
            Account newAccount = Account.builder()
                    .email(email)
                    .password(passwordEncoder.encode(superAdminPassword))
                    .roles(new HashSet<>(Set.of(superAdminRole)))
                    .build();

            Employee employee = Employee.builder()
                    .fullName("Super Admin")
                    .phone("")
                    .position("Super Admin")
                    .hotel(null)
                    .account(newAccount)
                    .build();

            employeeRepository.save(employee);
            return;
        }

        Set<Role> accountRoles = account.getRoles() == null
                ? new HashSet<>()
                : new HashSet<>(account.getRoles());

        if (accountRoles.add(superAdminRole)) {
            account.setRoles(accountRoles);
            accountRepository.save(account);
        }

        if (account.getEmployee() == null) {
            Employee employee = Employee.builder()
                    .fullName("Super Admin")
                    .phone("")
                    .position("Super Admin")
                    .hotel(null)
                    .account(account)
                    .build();

            employeeRepository.save(employee);
        }
    }
}