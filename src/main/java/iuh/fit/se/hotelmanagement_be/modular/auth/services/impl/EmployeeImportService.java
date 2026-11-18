package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.EmployeeExcelImportRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.EmployeeExcelRowRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.requests.UserRegisterRequest;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.CustomerRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.shared.entities.ImportTaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmployeeImportService {
    private final EmployeeServiceImpl employeeService;
    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final HotelRepository hotelRepository;
    private final EmployeeImportSocketEmitter employeeImportSocketEmitter;
    private final Map<String, ImportTaskStatus> taskStatusMap = new ConcurrentHashMap<>();

    public ImportTaskStatus getTaskStatus(String taskId) {
        return taskStatusMap.getOrDefault(taskId, new ImportTaskStatus(0, "Không tìm thấy tiến trình", "NOT_FOUND"));
    }

    public String startAsyncUrlImport(EmployeeExcelImportRequest request, Long hotelId, Account currentAccount) {
        if (request == null || request.getEmployees() == null || request.getEmployees().isEmpty()) {
            throw new IllegalArgumentException("Không có dữ liệu nhân viên để nhập.");
        }
        if (hotelId == null) {
            throw new IllegalArgumentException("Tài khoản chưa được gán chi nhánh.");
        }
        hotelRepository.findById(hotelId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh của tài khoản."));

        Set<String> emails = new java.util.HashSet<>();
        Set<String> phones = new java.util.HashSet<>();
        Set<String> cccds = new java.util.HashSet<>();
        for (EmployeeExcelRowRequest row : request.getEmployees()) {
            if (row == null || isBlank(row.getFullName()) || isBlank(row.getEmail())
                    || isBlank(row.getPhone()) || isBlank(row.getCccd())
                    || isBlank(row.getAddress()) || isBlank(row.getPosition())
                    || !isCloudinaryImageUrl(row.getAvatarUrl())) {
                throw new IllegalArgumentException("Dữ liệu nhân viên có thông tin bắt buộc bị thiếu hoặc URL ảnh không hợp lệ.");
            }
            if (!Set.of("Lễ tân", "Buồng phòng", "Bảo vệ").contains(row.getPosition().trim())) {
                throw new IllegalArgumentException("Chức vụ nhân viên không hợp lệ.");
            }
            String email = row.getEmail().trim().toLowerCase(Locale.ROOT);
            String phone = row.getPhone().trim();
            String cccd = row.getCccd().trim();
            if (!emails.add(email) || !phones.add(phone) || !cccds.add(cccd)
                    || accountRepository.existsByEmailIgnoreCase(row.getEmail().trim())
                    || customerRepository.existsByEmailIgnoreCase(row.getEmail().trim())
                    || employeeRepository.existsByPhone(phone)
                    || customerRepository.existsByPhone(phone)
                    || employeeRepository.existsByCccd(cccd)
                    || customerRepository.existsByCccd(cccd)) {
                throw new IllegalArgumentException("Email, số điện thoại hoặc CCCD bị trùng.");
            }
        }

        String taskId = UUID.randomUUID().toString();
        updateStatus(taskId, hotelId, 0, "Đang khởi tạo...", "PROCESSING", false);
        CompletableFuture.runAsync(() -> processImportTask(taskId, request.getEmployees(), hotelId, currentAccount));
        return taskId;
    }

    private void processImportTask(
            String taskId,
            List<EmployeeExcelRowRequest> rows,
            Long hotelId,
            Account currentAccount) {
        try {
            updateStatus(taskId, hotelId, 10, "Đang xác nhận chi nhánh...", "PROCESSING", false);
            Hotel hotel = hotelRepository.findById(hotelId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh của tài khoản."));
            int created = 0;
            for (int index = 0; index < rows.size(); index++) {
                EmployeeExcelRowRequest row = rows.get(index);
                UserRegisterRequest dto = UserRegisterRequest.builder()
                        .fullName(row.getFullName().trim())
                        .email(row.getEmail().trim())
                        .phone(row.getPhone().trim())
                        .cccd(row.getCccd().trim())
                        .address(row.getAddress().trim())
                        .position(row.getPosition().trim())
                        .hotelId(hotel.getId())
                        .avatarUrl(row.getAvatarUrl().trim())
                        .build();
                employeeService.createStaffAndAccount(dto, null, currentAccount);
                created++;
                int percent = 10 + (int) (((index + 1d) / rows.size()) * 85);
                updateStatus(taskId, hotelId, percent,
                        "Đang tạo nhân viên: " + (index + 1) + "/" + rows.size(),
                        "PROCESSING", false);
            }
            completeTask(taskId, hotelId, "Đã nhập thành công " + created + " nhân viên.");
        } catch (Exception exception) {
            failTask(taskId, hotelId, "Không thể nhập nhân viên: " + exception.getMessage());
        }
    }

    private boolean isCloudinaryImageUrl(String value) {
        if (isBlank(value)) return false;
        try {
            URI uri = URI.create(value.trim());
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "res.cloudinary.com".equalsIgnoreCase(uri.getHost())
                    && uri.getPath() != null
                    && uri.getPath().contains("/image/upload/");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void updateStatus(
            String taskId,
            Long hotelId,
            int percent,
            String message,
            String status,
            boolean completed) {
        taskStatusMap.put(taskId, new ImportTaskStatus(percent, message, "PROCESSING"));
        employeeImportSocketEmitter.emitImportProgress(hotelId, taskId, percent, message, status, completed);
    }

    private void completeTask(String taskId, Long hotelId, String message) {
        taskStatusMap.put(taskId, new ImportTaskStatus(100, message, "SUCCESS"));
        employeeImportSocketEmitter.emitImportProgress(hotelId, taskId, 100, message, "SUCCESS", true);
    }

    private void failTask(String taskId, Long hotelId, String errorMsg) {
        taskStatusMap.put(taskId, new ImportTaskStatus(0, errorMsg, "FAILED"));
        employeeImportSocketEmitter.emitImportProgress(hotelId, taskId, 0, errorMsg, "FAILED", true);
    }
}
