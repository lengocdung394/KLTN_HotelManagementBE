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
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import iuh.fit.se.hotelmanagement_be.modular.shift.entities.ShiftAssignment;
import iuh.fit.se.hotelmanagement_be.modular.shift.repositories.ShiftAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class HousekeepingTaskService {
    private static final List<String> ACTIVE_STATUSES = List.of("ASSIGNED", "REASSIGNED");
    private static final List<String> MANAGER_STATUSES = List.of("NEEDS_ASSIGNMENT", "ASSIGNED", "REASSIGNED");

    private final HousekeepingTaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final RoomRepository roomRepository;
    private final RoomSocketEmitter roomSocketEmitter;
    private final NotificationService notificationService;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final BookingSocketEmitter bookingSocketEmitter;

    @Transactional
    public void createTaskForCheckout(BookingDetail detail, Room room, LocalDateTime checkoutTime) {
        Long hotelId = room.getFloor().getBuilding().getHotel().getId();
        HousekeepingTask task = new HousekeepingTask();
        task.setRoom(room);
        task.setHotelId(hotelId);
        task.setBookingDetailId(detail.getId());
        task.setStatus("NEEDS_ASSIGNMENT");
        task.setCreatedAt(checkoutTime);
        taskRepository.save(task);
        notifyHotel(hotelId, task, "Phòng " + room.getRoomNumber() + " vừa checkout. Quản lý cần phân công nhân viên buồng phòng.");
    }

    @Transactional(readOnly = true)
    public List<HousekeepingTaskResponse> getHotelTasks() {
        Account account = currentAccount();
        Long hotelId = account.getHotelId();
        if (hotelId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản chưa được liên kết với chi nhánh.");
        }
        return taskRepository.findByHotelIdAndStatusInOrderByCreatedAtAsc(hotelId, MANAGER_STATUSES)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HousekeepingTaskResponse assignTask(String taskId, String employeeId) {
        Account account = currentAccount();
        Long hotelId = account.getHotelId();
        if (hotelId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản chưa được liên kết với chi nhánh.");
        }
        HousekeepingTask task = taskRepository.findById(taskId)
                .filter(candidate -> hotelId.equals(candidate.getHotelId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy việc dọn phòng trong chi nhánh."));
        if (!MANAGER_STATUSES.contains(task.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Việc dọn phòng này đã được xử lý.");
        }
        Employee employee = employeeRepository.findById(employeeId)
                .filter(candidate -> candidate.getHotel() != null && hotelId.equals(candidate.getHotel().getId()))
                .filter(candidate -> isHousekeeping(candidate.getPosition()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn nhân viên buồng phòng đang thuộc chi nhánh."));
        if (!isAssignedToHousekeepingShift(employee.getId(), hotelId, task.getCreatedAt())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ phân công được nhân viên buồng phòng đang trong ca tại thời điểm checkout."
            );
        }

        task.setAssignedEmployee(employee);
        task.setBackupEmployee(null);
        task.setAssignedAt(LocalDateTime.now());
        task.setReassignedAt(null);
        task.setStatus("ASSIGNED");
        taskRepository.save(task);
        notifyEmployee(employee, hotelId, task, NotificationType.HOUSEKEEPING_TASK_ASSIGNED,
                "Bạn được giao dọn phòng " + task.getRoom().getRoomNumber() + " sau checkout.");
        bookingSocketEmitter.emitHousekeepingTaskUpdate(hotelId);
        return toResponse(task);
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
                        task.getAssignedAt(),
                        task.getAssignedEmployee() == null ? null : task.getAssignedEmployee().getId(),
                        task.getAssignedEmployee() == null ? null : task.getAssignedEmployee().getFullName()))
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

        return markTaskCompleted(task);
    }

    @Transactional
    public HousekeepingTaskResponse completeTaskByManager(String taskId) {
        Account account = currentAccount();
        Long hotelId = account.getHotelId();
        if (hotelId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản chưa được liên kết với chi nhánh.");
        }
        HousekeepingTask task = taskRepository.findById(taskId)
                .filter(candidate -> hotelId.equals(candidate.getHotelId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy việc dọn phòng trong chi nhánh."));
        if (!ACTIVE_STATUSES.contains(task.getStatus()) || task.getAssignedEmployee() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ xác nhận hoàn tất được công việc đã phân công.");
        }

        return markTaskCompleted(task);
    }

    private HousekeepingTaskResponse markTaskCompleted(HousekeepingTask task) {
        LocalDateTime completedAt = LocalDateTime.now();
        task.setStatus("COMPLETED");
        task.setCompletedAt(completedAt);
        Room room = task.getRoom();
        room.setRoomStatus(RoomStatus.READY);
        roomRepository.save(room);
        taskRepository.save(task);
        roomSocketEmitter.emitRoomStatusUpdated(
                task.getHotelId(), room.getId(), room.getRoomNumber(), RoomStatus.READY, "HOUSEKEEPING_COMPLETED");
        notificationService.createAndPublish(
                task.getHotelId(),
                NotificationType.HOUSEKEEPING_TASK_COMPLETED,
                "Đã xác nhận dọn phòng",
                "Phòng " + room.getRoomNumber() + " đã được dọn xong và chuyển sang trạng thái sẵn sàng.",
                "HOUSEKEEPING_TASK",
                task.getId());
        bookingSocketEmitter.emitHousekeepingTaskUpdate(task.getHotelId());
        return new HousekeepingTaskResponse(
                task.getId(), room.getId(), room.getRoomNumber(),
                task.getBookingDetailId(), task.getStatus(), task.getCreatedAt(), task.getAssignedAt(),
                task.getAssignedEmployee() == null ? null : task.getAssignedEmployee().getId(),
                task.getAssignedEmployee() == null ? null : task.getAssignedEmployee().getFullName());
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
                bookingSocketEmitter.emitHousekeepingTaskUpdate(task.getHotelId());
                continue;
            }
            task.setAssignedEmployee(backup);
            task.setStatus("REASSIGNED");
            task.setAssignedAt(LocalDateTime.now());
            task.setReassignedAt(LocalDateTime.now());
            taskRepository.save(task);
            notifyEmployee(backup, task.getHotelId(), task, NotificationType.HOUSEKEEPING_TASK_REASSIGNED,
                    "Việc dọn phòng " + task.getRoom().getRoomNumber() + " được chuyển cho bạn. Vui lòng hoàn tất và xác nhận.");
            bookingSocketEmitter.emitHousekeepingTaskUpdate(task.getHotelId());
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

    private HousekeepingTaskResponse toResponse(HousekeepingTask task) {
        Employee assignedEmployee = task.getAssignedEmployee();
        return new HousekeepingTaskResponse(
                task.getId(),
                task.getRoom().getId(),
                task.getRoom().getRoomNumber(),
                task.getBookingDetailId(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getAssignedAt(),
                assignedEmployee == null ? null : assignedEmployee.getId(),
                assignedEmployee == null ? null : assignedEmployee.getFullName()
        );
    }

    private boolean isHousekeeping(String roleOrPosition) {
        if (roleOrPosition == null) return false;
        String value = roleOrPosition.trim().toLowerCase(Locale.ROOT);
        return value.contains("buồng") || value.contains("buong") || value.contains("housekeeping") || value.contains("housekeeper");
    }

    private boolean isAssignedToHousekeepingShift(
            String employeeId,
            Long hotelId,
            LocalDateTime checkoutTime
    ) {
        if (checkoutTime == null) return false;

        LocalDate workDate = checkoutTime.toLocalDate();
        if (checkoutTime.toLocalTime().isBefore(LocalTime.of(6, 0))) {
            workDate = workDate.minusDays(1);
        }
        LocalTime checkoutLocalTime = checkoutTime.toLocalTime();

        return shiftAssignmentRepository.findByHotelIdAndWorkDate(hotelId, workDate).stream()
                .filter(assignment -> assignment.getEmployee() != null
                        && employeeId.equals(assignment.getEmployee().getId()))
                .filter(assignment -> "SCHEDULED".equalsIgnoreCase(assignment.getStatus()))
                .filter(assignment -> isHousekeeping(assignment.getRole()))
                .anyMatch(assignment -> shiftContains(assignment.getShiftTime(), checkoutLocalTime));
    }

    private boolean shiftContains(String shiftTime, LocalTime time) {
        if (shiftTime == null) return false;

        Matcher matcher = Pattern.compile("(\\d{1,2}):(\\d{2})").matcher(shiftTime);
        if (!matcher.find()) return false;
        LocalTime start = LocalTime.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        if (!matcher.find()) return false;
        LocalTime end = LocalTime.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));

        return start.isBefore(end)
                ? !time.isBefore(start) && time.isBefore(end)
                : !time.isBefore(start) || time.isBefore(end);
    }
}
