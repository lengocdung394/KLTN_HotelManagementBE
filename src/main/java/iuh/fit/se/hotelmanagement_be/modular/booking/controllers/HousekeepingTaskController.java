package iuh.fit.se.hotelmanagement_be.modular.booking.controllers;

import iuh.fit.se.hotelmanagement_be.modular.booking.responses.HousekeepingTaskResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.impl.HousekeepingTaskService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/housekeeping-tasks")
public class HousekeepingTaskController {
    private final HousekeepingTaskService housekeepingTaskService;

    @PreAuthorize("hasAuthority('ROLE_EMPLOYEE')")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<HousekeepingTaskResponse>>> getMyTasks() {
        return ResponseEntity.ok(ApiResponse.<List<HousekeepingTaskResponse>>builder()
                .code(1000).message("Lấy danh sách việc dọn phòng thành công")
                .result(housekeepingTaskService.getMyTasks()).build());
    }

    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN')")
    @GetMapping("/management")
    public ResponseEntity<ApiResponse<List<HousekeepingTaskResponse>>> getHotelTasks() {
        return ResponseEntity.ok(ApiResponse.<List<HousekeepingTaskResponse>>builder()
                .code(1000).message("Lấy danh sách công việc dọn phòng thành công")
                .result(housekeepingTaskService.getHotelTasks()).build());
    }

    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN')")
    @PutMapping("/{taskId}/assign")
    public ResponseEntity<ApiResponse<HousekeepingTaskResponse>> assignTask(
            @PathVariable String taskId,
            @RequestParam String employeeId
    ) {
        return ResponseEntity.ok(ApiResponse.<HousekeepingTaskResponse>builder()
                .code(1000).message("Đã phân công nhân viên dọn phòng")
                .result(housekeepingTaskService.assignTask(taskId, employeeId)).build());
    }

    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN')")
    @PostMapping("/{taskId}/management-complete")
    public ResponseEntity<ApiResponse<HousekeepingTaskResponse>> completeTaskByManager(@PathVariable String taskId) {
        return ResponseEntity.ok(ApiResponse.<HousekeepingTaskResponse>builder()
                .code(1000).message("Đã xác nhận dọn phòng hoàn tất")
                .result(housekeepingTaskService.completeTaskByManager(taskId)).build());
    }

    @PreAuthorize("hasAuthority('ROLE_EMPLOYEE')")
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<ApiResponse<HousekeepingTaskResponse>> completeTask(@PathVariable String taskId) {
        return ResponseEntity.ok(ApiResponse.<HousekeepingTaskResponse>builder()
                .code(1000).message("Xác nhận dọn phòng thành công")
                .result(housekeepingTaskService.completeTask(taskId)).build());
    }
}
