package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.config.SecurityUtils;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.HousekeepingTask;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.HousekeepingTaskRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.HousekeepingTaskResponse;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import iuh.fit.se.hotelmanagement_be.modular.notification.services.NotificationService;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus;
import iuh.fit.se.hotelmanagement_be.modular.room.services.impl.RoomSocketEmitter;
import iuh.fit.se.hotelmanagement_be.modular.shift.entities.ShiftAssignment;
import iuh.fit.se.hotelmanagement_be.modular.shift.repositories.ShiftAssignmentRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class HousekeepingTaskService {
    private static final List<String> ACTIVE_STATUSES = List.of("ASSIGNED", "REASSIGNED");

    private final HousekeepingTaskRepository taskRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final RoomRepository roomRepository;
    private final RoomSocketEmitter roomSocketEmitter;
    private final NotificationService notificationService;

    @Transactional
    public void createTaskForCheckout(BookingDetail detail, Room room, LocalDateTime checkoutTime) {
        Long hotelId = room.getFloor().getBuilding().getHotel().getId();
        LocalDate workDate = checkoutTime.toLocalDate();
        String shiftType = shiftForTime(checkoutTime.toLocalTime());
        if (shiftType.equals("Ca đêm") && checkoutTime.toLocalTime().isBefore(LocalTime.of(6, 0))) {
            workDate = workDate.minusDays(1);
        }

        Set<String> scheduledEmployeeIds = new HashSet<>();
        List<Employee> scheduledHousekeepers = shiftAssignmentRepository
                .findByHotelIdAndWorkDate(hotelId, workDate).stream()
                .filter(assignment -> isHousekeeping(assignment.getRole()))
                .filter(assignment -> shiftType.equals(normalizeShift(assignment.getShiftType())))
                .map(ShiftAssignment::getEmployee)
                .filter(employee -> employee != null && scheduledEmployeeIds.add(employee.getId()))
                .toList();

        List<Employee> assignedHousekeepers = new ArrayList<>(scheduledHousekeepers);
        if (scheduledHousekeepers.size() < 2) {
            List<Employee> fallback = employeeRepository.findByHotelId(hotelId).stream()
                    .filter(employee -> isHousekeeping(employee.getPosition()))
                    .filter(employee -> scheduledHousekeepers.stream().noneMatch(assigned -> assigned.getId().equals(employee.getId())))
                    .toList();
            assignedHousekeepers.addAll(fallback);
        }

        Employee primary = assignedHousekeepers.isEmpty()
                ? null
                : assignedHousekeepers.get(ThreadLocalRandom.current().nextInt(Math.min(2, assignedHousekeepers.size())));
        Employee backup = assignedHousekeepers.stream()
                .filter(employee -> primary == null || !employee.getId().equals(primary.getId()))
                .findFirst()
                .orElse(null);

        HousekeepingTask task = new HousekeepingTask();
        task.setRoom(room);
        task.setHotelId(hotelId);
        task.setBookingDetailId(detail.getId());
        task.setAssignedEmployee(primary);
        task.setBackupEmployee(backup);
        task.setStatus(primary == null ? "NEEDS_ASSIGNMENT" : "ASSIGNED");
        task.setCreatedAt(checkoutTime);
        task.setAssignedAt(primary == null ? null : checkoutTime);
        taskRepository.save(task);

        if (primary == null) {
            notifyHotel(hotelId, task, "Chưa có nhân viên buồng phòng được phân ca cho phòng " + room.getRoomNumber() + ".");
        } else {
            notifyEmployee(primary, hotelId, task, NotificationType.HOUSEKEEPING_TASK_ASSIGNED,
                    "Phòng " + room.getRoomNumber() + " vừa checkout, vui lòng dọn và xác nhận hoàn tất trong 1 giờ.");
        }
    }

    @Transactional(readOnly = true)
    public List<HousekeepingTaskResponse> getMyTasks() {
        Account account = currentAccount();
        if (account.getEmployee() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản không có hồ sơ nhân viên.");
        }
        return taskRepository.findByAssignedEmployee_IdAndStatusInOrderByCreatedAtAsc(
                        account.getEmployee().getId(), ACTIVE_STATUSES)
                .stream()
                .map(task -> new HousekeepingTaskResponse(
                        task.getId(),
                        task.getRoom().getId(),
                        task.getRoom().getRoomNumber(),
                        task.getBookingDetailId(),
                        task.getStatus(),
                        task.getCreatedAt(),
                        task.getAssignedAt()))
                .toList();
    }

    @Transactional
    public HousekeepingTaskResponse completeTask(String taskId) {
        Account account = currentAccount();
        Employee employee = account.getEmployee();
        if (employee == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản không có hồ sơ nhân viên.");
        }
        HousekeepingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy việc dọn phòng."));
        if (task.getAssignedEmployee() == null || !task.getAssignedEmployee().getId().equals(employee.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Việc dọn phòng này không được giao cho bạn.");
        }
        if (!ACTIVE_STATUSES.contains(task.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Việc dọn phòng đã được xử lý.");
        }

        LocalDateTime completedAt = LocalDateTime.now();
        task.setStatus("COMPLETED");
        task.setCompletedAt(completedAt);
        Room room = task.getRoom();
        room.setRoomStatus(RoomStatus.READY);
        roomRepository.save(room);
        taskRepository.save(task);
        roomSocketEmitter.emitRoomStatusUpdated(
                task.getHotelId(), room.getId(), room.getRoomNumber(), RoomStatus.READY);
        notificationService.createAndPublish(
                task.getHotelId(),
                NotificationType.HOUSEKEEPING_TASK_COMPLETED,
                "Đã xác nhận dọn phòng",
                "Phòng " + room.getRoomNumber() + " đã được dọn xong và chuyển sang trạng thái sẵn sàng.",
                "HOUSEKEEPING_TASK",
                task.getId());
        return new HousekeepingTaskResponse(
                task.getId(), room.getId(), room.getRoomNumber(),
                task.getBookingDetailId(), task.getStatus(), task.getCreatedAt(), task.getAssignedAt());
    }

    @Transactional
    public void reassignOverdueTasks() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        for (HousekeepingTask task : taskRepository.findTasksAwaitingReassignment(cutoff)) {
            Employee backup = task.getBackupEmployee();
            if (backup == null || task.getAssignedEmployee() == null
                    || backup.getId().equals(task.getAssignedEmployee().getId())) {
                task.setStatus("NEEDS_ASSIGNMENT");
                taskRepository.save(task);
                notifyHotel(task.getHotelId(), task,
                        "Phòng " + task.getRoom().getRoomNumber() + " chưa được xác nhận dọn sau 1 giờ; cần quản lý phân công lại.");
                continue;
            }
            task.setAssignedEmployee(backup);
            task.setStatus("REASSIGNED");
            task.setAssignedAt(LocalDateTime.now());
            task.setReassignedAt(LocalDateTime.now());
            taskRepository.save(task);
            notifyEmployee(backup, task.getHotelId(), task, NotificationType.HOUSEKEEPING_TASK_REASSIGNED,
                    "Việc dọn phòng " + task.getRoom().getRoomNumber() + " được chuyển cho bạn. Vui lòng hoàn tất và xác nhận.");
        }
    }

    private Account currentAccount() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác định được tài khoản đăng nhập.");
        }
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản đăng nhập."));
    }

    private void notifyEmployee(
            Employee employee,
            Long hotelId,
            HousekeepingTask task,
            NotificationType type,
            String message
    ) {
        if (employee.getAccount() == null || employee.getAccount().getId() == null) {
            notifyHotel(hotelId, task, message);
            return;
        }
        notificationService.createAndPublishToAccount(
                employee.getAccount().getId(), hotelId, type,
                "Yêu cầu dọn phòng", message, "HOUSEKEEPING_TASK", task.getId());
    }

    private void notifyHotel(Long hotelId, HousekeepingTask task, String message) {
        notificationService.createAndPublish(
                hotelId, NotificationType.SYSTEM, "Cần phân công dọn phòng",
                message, "HOUSEKEEPING_TASK", task.getId());
    }

    private boolean isHousekeeping(String roleOrPosition) {
        if (roleOrPosition == null) return false;
        String value = roleOrPosition.trim().toLowerCase(Locale.ROOT);
        return value.contains("buồng") || value.contains("housekeeping") || value.contains("housekeeper");
    }

    private String normalizeShift(String shift) {
        if (shift == null) return "";
        String value = shift.trim().toLowerCase(Locale.ROOT);
        if (value.equals("ca sáng") || value.equals("morning")) return "Ca sáng";
        if (value.equals("ca chiều") || value.equals("ca tối") || value.equals("afternoon") || value.equals("evening")) return "Ca chiều";
        if (value.equals("ca đêm") || value.equals("night")) return "Ca đêm";
        return shift;
    }

    private String shiftForTime(LocalTime time) {
        if (!time.isBefore(LocalTime.of(6, 0)) && time.isBefore(LocalTime.of(14, 0))) return "Ca sáng";
        if (!time.isBefore(LocalTime.of(14, 0)) && time.isBefore(LocalTime.of(22, 0))) return "Ca chiều";
        return "Ca đêm";
    }
}
