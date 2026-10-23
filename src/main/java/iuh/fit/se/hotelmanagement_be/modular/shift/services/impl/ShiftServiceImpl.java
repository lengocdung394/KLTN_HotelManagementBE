package iuh.fit.se.hotelmanagement_be.modular.shift.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.shift.entities.ShiftAssignment;
import iuh.fit.se.hotelmanagement_be.modular.shift.repositories.ShiftAssignmentRepository;
import iuh.fit.se.hotelmanagement_be.modular.shift.requests.ShiftAssignRequest;
import iuh.fit.se.hotelmanagement_be.modular.shift.requests.ShiftBatchAssignRequest;
import iuh.fit.se.hotelmanagement_be.modular.shift.responses.DailyShiftSummaryResponse;
import iuh.fit.se.hotelmanagement_be.modular.shift.responses.ShiftAssignmentResponse;
import iuh.fit.se.hotelmanagement_be.modular.shift.responses.WeeklyScheduleResponse;
import iuh.fit.se.hotelmanagement_be.modular.shift.services.ShiftService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShiftServiceImpl implements ShiftService {

    private static final ZoneId HOTEL_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    ShiftAssignmentRepository shiftAssignmentRepository;
    EmployeeRepository employeeRepository;
    HotelRepository hotelRepository;

    @Override
    public List<ShiftAssignmentResponse> getTodayShifts(Long hotelId, LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        List<ShiftAssignment> assignments = shiftAssignmentRepository.findByHotelIdAndWorkDate(hotelId, targetDate);

        return assignments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DailyShiftSummaryResponse getDailyShiftSummary(Long hotelId, LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        List<ShiftAssignmentResponse> list = getTodayShifts(hotelId, targetDate);
        return buildDailySummary(targetDate, list);
    }

    @Override
    public WeeklyScheduleResponse getWeeklySchedule(Long hotelId, LocalDate weekStartDate) {
        LocalDate monday = (weekStartDate != null)
                ? weekStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        List<ShiftAssignment> assignments = shiftAssignmentRepository.findByHotelIdAndWorkDateBetween(hotelId, monday, sunday);

        Map<LocalDate, List<ShiftAssignmentResponse>> groupedByDate = assignments.stream()
                .map(this::toResponse)
                .collect(Collectors.groupingBy(ShiftAssignmentResponse::getWorkDate));

        List<DailyShiftSummaryResponse> days = new ArrayList<>();
        int daysFullyStaffed = 0;
        Set<String> uniqueStaff = new HashSet<>();

        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            List<ShiftAssignmentResponse> dayShifts = groupedByDate.getOrDefault(d, Collections.emptyList());
            DailyShiftSummaryResponse dailySummary = buildDailySummary(d, dayShifts);
            if (dailySummary.isFullyStaffed()) {
                daysFullyStaffed++;
            }
            dayShifts.stream()
                    .filter(s -> s.getEmployeeId() != null)
                    .forEach(s -> uniqueStaff.add(s.getEmployeeId()));
            days.add(dailySummary);
        }

        return WeeklyScheduleResponse.builder()
                .hotelId(hotelId)
                .weekStartDate(monday)
                .weekEndDate(sunday)
                .daysFullyStaffed(daysFullyStaffed)
                .totalAssignments(assignments.size())
                .totalUniqueStaff(uniqueStaff.size())
                .days(days)
                .build();
    }

    @Override
    @Transactional
    public ShiftAssignmentResponse assignShift(ShiftAssignRequest request, Long hotelId) {
        requireEditableDate(request.getWorkDate());
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn với ID: " + hotelId));

        Employee employee = null;
        if (request.getEmployeeId() != null && !request.getEmployeeId().isBlank()) {
            employee = employeeRepository.findById(request.getEmployeeId())
                    .orElse(null);
        }
        if (employee == null && request.getEmployeeName() != null && !request.getEmployeeName().isBlank()) {
            employee = employeeRepository.findFirstByFullName(request.getEmployeeName().trim())
                    .orElse(null);
        }

        String defaultShiftTime = request.getShiftTime();
        if (defaultShiftTime == null || defaultShiftTime.isBlank()) {
            defaultShiftTime = request.getShiftType().contains("sáng") || request.getShiftType().equalsIgnoreCase("MORNING")
                    ? "06:00 – 14:00"
                    : "14:00 – 22:00";
        }

        // Tìm xem đã có phân ca tại vị trí + ca này chưa
        List<ShiftAssignment> existingAssignments = shiftAssignmentRepository
                .findAllByHotel_IdAndWorkDateAndRoleAndShiftType(hotelId, request.getWorkDate(), request.getRole(), request.getShiftType());

        Employee assignedEmployee = employee;
        ShiftAssignment shift;
        Optional<ShiftAssignment> existingOpt = existingAssignments.stream()
                .filter(existing -> existing.getEmployee() != null
                        && assignedEmployee != null
                        && existing.getEmployee().getId().equals(assignedEmployee.getId()))
                .findFirst();
        if (existingOpt.isPresent()) {
            shift = existingOpt.get();
            shift.setEmployee(employee);
            shift.setShiftTime(defaultShiftTime);
            shift.setTask(request.getTask());
            shift.setNote(request.getNote());
        } else {
            shift = ShiftAssignment.builder()
                    .hotel(hotel)
                    .employee(employee)
                    .workDate(request.getWorkDate())
                    .shiftType(request.getShiftType())
                    .shiftTime(defaultShiftTime)
                    .role(request.getRole())
                    .task(request.getTask())
                    .note(request.getNote())
                    .status("SCHEDULED")
                    .build();
        }

        ShiftAssignment saved = shiftAssignmentRepository.save(shift);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public List<ShiftAssignmentResponse> assignBatchShifts(ShiftBatchAssignRequest request, Long hotelId) {
        if (request.getAssignments() == null || request.getAssignments().isEmpty()) {
            return Collections.emptyList();
        }

        Long targetHotelId = (hotelId != null) ? hotelId : (request.getHotelId() != null ? request.getHotelId() : 1L);

        List<ShiftAssignmentResponse> results = new ArrayList<>();
        for (ShiftAssignRequest item : request.getAssignments()) {
            Long itemHotelId = item.getHotelId() != null ? item.getHotelId() : targetHotelId;
            results.add(assignShift(item, itemHotelId));
        }
        return results;
    }

    @Override
    @Transactional
    public WeeklyScheduleResponse replaceWeeklySchedule(
            ShiftBatchAssignRequest request,
            Long hotelId,
            LocalDate weekStartDate
    ) {
        LocalDate monday = weekStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        LocalDate today = today();
        if (!sunday.isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể chỉnh sửa lịch của hôm nay hoặc ngày đã qua.");
        }
        LocalDate firstEditableDate = today.plusDays(1);
        LocalDate editableStart = firstEditableDate.isAfter(monday) ? firstEditableDate : monday;
        Map<String, List<ShiftAssignRequest>> slots = new HashMap<>();
        Set<String> employeesPerDay = new HashSet<>();
        for (ShiftAssignRequest assignment : request.getAssignments()) {
            if (assignment.getWorkDate().isBefore(monday) || assignment.getWorkDate().isAfter(sunday)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lịch phân ca phải nằm trong tuần đã chọn.");
            }
            if (!assignment.getWorkDate().isAfter(today)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể chỉnh sửa phân công của hôm nay hoặc ngày đã qua.");
            }
            String role = normalizeRole(assignment.getRole());
            if (!List.of("Lễ tân", "Buồng phòng", "Bảo vệ").contains(role)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vị trí ca làm không hợp lệ.");
            }
            String shiftType = normalizeShiftType(assignment.getShiftType());
            if (shiftType == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ca làm phải là Ca sáng, Ca chiều hoặc Ca đêm.");
            }
            assignment.setRole(role);
            assignment.setShiftType(shiftType);
            String slotKey = assignment.getWorkDate() + "|" + shiftType + "|" + role;
            slots.computeIfAbsent(slotKey, ignored -> new ArrayList<>()).add(assignment);

            if (assignment.getEmployeeId() == null || assignment.getEmployeeId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mỗi vị trí trong ca phải được phân công nhân viên.");
            }
            String employeeDayKey = assignment.getWorkDate() + "|" + assignment.getEmployeeId();
            if (!employeesPerDay.add(employeeDayKey)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Một nhân viên không thể được phân hai vị trí trong cùng ngày.");
            }
        }

        for (LocalDate workDate = editableStart; !workDate.isAfter(sunday); workDate = workDate.plusDays(1)) {
            for (String shiftType : List.of("Ca sáng", "Ca chiều", "Ca đêm")) {
                requireSlotCount(slots, workDate, shiftType, "Lễ tân", 2);
                requireSlotCount(slots, workDate, shiftType, "Buồng phòng", 2);
                requireSlotCount(slots, workDate, shiftType, "Bảo vệ", 1);
            }
        }

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách sạn."));
        List<ShiftAssignment> assignments = new ArrayList<>();
        for (ShiftAssignRequest item : request.getAssignments()) {
            Employee employee = employeeRepository.findById(item.getEmployeeId())
                    .filter(candidate -> candidate.getHotel() != null && hotelId.equals(candidate.getHotel().getId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhân viên không thuộc chi nhánh hiện tại."));
            if (!employeeMatchesRole(employee, item.getRole())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chức vụ nhân viên không khớp với vị trí được phân công.");
            }
            assignments.add(ShiftAssignment.builder()
                    .hotel(hotel)
                    .employee(employee)
                    .workDate(item.getWorkDate())
                    .shiftType(item.getShiftType())
                    .shiftTime(shiftTime(item.getShiftType()))
                    .role(item.getRole())
                    .task(item.getTask())
                    .note(item.getNote())
                    .status("SCHEDULED")
                    .build());
        }

        shiftAssignmentRepository.deleteByHotel_IdAndWorkDateBetween(hotelId, editableStart, sunday);
        shiftAssignmentRepository.saveAll(assignments);
        return getWeeklySchedule(hotelId, monday);
    }

    @Override
    @Transactional
    public WeeklyScheduleResponse clearWeeklySchedule(Long hotelId, LocalDate weekStartDate) {
        LocalDate monday = weekStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        LocalDate today = today();
        if (!sunday.isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể xóa lịch của hôm nay hoặc ngày đã qua.");
        }
        LocalDate firstEditableDate = today.plusDays(1);
        LocalDate editableStart = firstEditableDate.isAfter(monday) ? firstEditableDate : monday;
        shiftAssignmentRepository.deleteByHotel_IdAndWorkDateBetween(hotelId, editableStart, sunday);
        return getWeeklySchedule(hotelId, monday);
    }

    @Override
    @Transactional
    public WeeklyScheduleResponse copyWeeklySchedule(Long hotelId, LocalDate sourceWeekStartDate, LocalDate targetWeekStartDate) {
        LocalDate sourceMonday = sourceWeekStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate targetMonday = targetWeekStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate targetSunday = targetMonday.plusDays(6);
        LocalDate today = today();
        if (sourceMonday.equals(targetMonday)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tuần nguồn và tuần đích phải khác nhau.");
        }
        if (!targetSunday.isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể sao chép vào hôm nay hoặc ngày đã qua.");
        }

        List<ShiftAssignment> sourceAssignments = shiftAssignmentRepository.findByHotelIdAndWorkDateBetween(
                hotelId, sourceMonday, sourceMonday.plusDays(6));
        if (sourceAssignments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tuần nguồn chưa có lịch phân ca để sao chép.");
        }

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách sạn."));
        LocalDate firstEditableDate = today.plusDays(1);
        LocalDate editableStart = firstEditableDate.isAfter(targetMonday) ? firstEditableDate : targetMonday;
        List<ShiftAssignment> copiedAssignments = new ArrayList<>();
        Set<String> employeeDays = new HashSet<>();
        for (ShiftAssignment source : sourceAssignments) {
            LocalDate targetDate = targetMonday.plusDays(java.time.temporal.ChronoUnit.DAYS.between(sourceMonday, source.getWorkDate()));
            if (targetDate.isBefore(editableStart) || targetDate.isAfter(targetSunday)) continue;
            Employee employee = source.getEmployee();
            String role = normalizeRole(source.getRole());
            String shiftType = normalizeShiftType(source.getShiftType());
            if (!List.of("Lễ tân", "Buồng phòng", "Bảo vệ").contains(role) || shiftType == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lịch nguồn có vị trí hoặc ca làm không hợp lệ.");
            }
            if (employee == null || employee.getHotel() == null || !hotelId.equals(employee.getHotel().getId())
                    || !employeeMatchesRole(employee, role)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lịch nguồn có nhân viên không hợp lệ với chi nhánh hoặc vị trí.");
            }
            if (!employeeDays.add(targetDate + "|" + employee.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lịch nguồn có nhân viên bị phân nhiều ca trong cùng ngày.");
            }
            copiedAssignments.add(ShiftAssignment.builder()
                    .hotel(hotel)
                    .employee(employee)
                    .workDate(targetDate)
                    .shiftType(shiftType)
                    .shiftTime(source.getShiftTime())
                    .role(role)
                    .task(source.getTask())
                    .note(source.getNote())
                    .status("SCHEDULED")
                    .build());
        }

        shiftAssignmentRepository.deleteByHotel_IdAndWorkDateBetween(hotelId, editableStart, targetSunday);
        shiftAssignmentRepository.saveAll(copiedAssignments);
        return getWeeklySchedule(hotelId, targetMonday);
    }

    @Override
    @Transactional
    public void deleteShift(String shiftId) {
        shiftAssignmentRepository.findById(shiftId).ifPresent(assignment -> {
            requireEditableDate(assignment.getWorkDate());
            shiftAssignmentRepository.delete(assignment);
        });
    }

    private LocalDate today() {
        return LocalDate.now(HOTEL_TIME_ZONE);
    }

    private void requireEditableDate(LocalDate workDate) {
        if (workDate == null || !workDate.isAfter(today())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể chỉnh sửa phân công của hôm nay hoặc ngày đã qua.");
        }
    }

    private void requireSlotCount(
            Map<String, List<ShiftAssignRequest>> slots,
            LocalDate date,
            String shiftType,
            String role,
            int required
    ) {
        int actual = slots.getOrDefault(date + "|" + shiftType + "|" + role, List.of()).size();
        if (actual != required) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    date + " " + shiftType + " cần đúng " + required + " nhân viên " + role + "."
            );
        }
    }

    private String normalizeRole(String role) {
        if (role == null) return "";
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("lễ tân") || normalized.contains("front desk") || normalized.contains("reception")) return "Lễ tân";
        if (normalized.contains("buồng") || normalized.contains("housekeeping") || normalized.contains("house keeper")) return "Buồng phòng";
        if (normalized.contains("bảo vệ") || normalized.contains("security") || normalized.contains("guard")) return "Bảo vệ";
        return role.trim();
    }

    private String normalizeShiftType(String shiftType) {
        if (shiftType == null) return null;
        return switch (shiftType.trim().toLowerCase(Locale.ROOT)) {
            case "ca sáng", "morning", "shift 1" -> "Ca sáng";
            case "ca chiều", "afternoon", "ca tối", "evening", "shift 2" -> "Ca chiều";
            case "ca đêm", "night", "shift 3" -> "Ca đêm";
            default -> null;
        };
    }

    private String shiftTime(String shiftType) {
        return switch (shiftType) {
            case "Ca sáng" -> "06:00 – 14:00";
            case "Ca chiều" -> "14:00 – 22:00";
            case "Ca đêm" -> "22:00 – 06:00";
            default -> throw new IllegalArgumentException("Unknown shift type " + shiftType);
        };
    }

    private boolean employeeMatchesRole(Employee employee, String role) {
        String position = employee.getPosition() == null ? "" : employee.getPosition().trim().toLowerCase(Locale.ROOT);
        return switch (role) {
            case "Lễ tân" -> position.contains("lễ tân") || position.contains("reception") || position.contains("front desk") || position.contains("front office");
            case "Buồng phòng" -> position.contains("buồng") || position.contains("housekeeping") || position.contains("housekeeper") || position.contains("room attendant");
            case "Bảo vệ" -> position.contains("bảo vệ") || position.contains("security") || position.contains("guard");
            default -> false;
        };
    }

    private DailyShiftSummaryResponse buildDailySummary(LocalDate date, List<ShiftAssignmentResponse> assignments) {
        boolean fullyStaffed = assignments.size() == 15 && assignments.stream().allMatch(a -> a.getEmployeeId() != null);
        return DailyShiftSummaryResponse.builder()
                .date(date)
                .dayKey(getDayKey(date))
                .dayLabel(getDayLabel(date))
                .dateDisplay(date.format(DateTimeFormatter.ofPattern("dd/MM")))
                .fullyStaffed(fullyStaffed)
                .totalAssigned(assignments.size())
                .assignments(assignments)
                .build();
    }

    private ShiftAssignmentResponse toResponse(ShiftAssignment entity) {
        Employee emp = entity.getEmployee();
        String empName = (emp != null) ? emp.getFullName() : "Chưa phân công";
        String empId = (emp != null) ? emp.getId() : null;
        String empPhone = (emp != null) ? emp.getPhone() : null;
        String avatar = (emp != null) ? emp.getAvatarUrl() : null;
        String position = (emp != null) ? emp.getPosition() : null;

        return ShiftAssignmentResponse.builder()
                .id(entity.getId())
                .employeeId(empId)
                .employeeName(empName)
                .employeePhone(empPhone)
                .avatarUrl(avatar)
                .initials(getInitials(empName))
                .position(position)
                .hotelId(entity.getHotel().getId())
                .workDate(entity.getWorkDate())
                .dayKey(getDayKey(entity.getWorkDate()))
                .dayLabel(getDayLabel(entity.getWorkDate()))
                .dateDisplay(entity.getWorkDate().format(DateTimeFormatter.ofPattern("dd/MM")))
                .shiftType(entity.getShiftType())
                .shiftTime(entity.getShiftTime())
                .role(entity.getRole())
                .task(entity.getTask())
                .status(entity.getStatus())
                .note(entity.getNote())
                .build();
    }

    private String getDayKey(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "mon";
            case TUESDAY -> "tue";
            case WEDNESDAY -> "wed";
            case THURSDAY -> "thu";
            case FRIDAY -> "fri";
            case SATURDAY -> "sat";
            case SUNDAY -> "sun";
        };
    }

    private String getDayLabel(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "Thứ 2";
            case TUESDAY -> "Thứ 3";
            case WEDNESDAY -> "Thứ 4";
            case THURSDAY -> "Thứ 5";
            case FRIDAY -> "Thứ 6";
            case SATURDAY -> "Thứ 7";
            case SUNDAY -> "Chủ nhật";
        };
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "NV";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[parts.length - 2].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
