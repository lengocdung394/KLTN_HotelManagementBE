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

    @PreAuthorize("hasAuthority('ROLE_EMPLOYEE')")
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<ApiResponse<HousekeepingTaskResponse>> completeTask(@PathVariable String taskId) {
        return ResponseEntity.ok(ApiResponse.<HousekeepingTaskResponse>builder()
                .code(1000).message("Xác nhận dọn phòng thành công")
                .result(housekeepingTaskService.completeTask(taskId)).build());
    }
}
