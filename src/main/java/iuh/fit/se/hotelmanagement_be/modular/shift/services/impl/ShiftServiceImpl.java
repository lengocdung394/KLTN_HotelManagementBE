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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShiftServiceImpl implements ShiftService {

    ShiftAssignmentRepository shiftAssignmentRepository;
    EmployeeRepository employeeRepository;
    HotelRepository hotelRepository;

    @Override
    public List<ShiftAssignmentResponse> getTodayShifts(Long hotelId, LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        List<ShiftAssignment> assignments = shiftAssignmentRepository.findByHotelIdAndWorkDate(hotelId, targetDate);

        if (assignments.isEmpty()) {
            initDefaultWeeklyScheduleIfEmpty(hotelId, targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
            assignments = shiftAssignmentRepository.findByHotelIdAndWorkDate(hotelId, targetDate);
        }

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

        if (assignments.isEmpty()) {
            initDefaultWeeklyScheduleIfEmpty(hotelId, monday);
            assignments = shiftAssignmentRepository.findByHotelIdAndWorkDateBetween(hotelId, monday, sunday);
        }

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
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn với ID: " + hotelId));

        Employee employee = null;
        if (request.getEmployeeId() != null && !request.getEmployeeId().isBlank()) {
            employee = employeeRepository.findById(request.getEmployeeId())
                    .orElse(null);
        }

        String defaultShiftTime = request.getShiftTime();
        if (defaultShiftTime == null || defaultShiftTime.isBlank()) {
            defaultShiftTime = request.getShiftType().contains("sáng") || request.getShiftType().equalsIgnoreCase("MORNING")
                    ? "06:00 – 14:00"
                    : "14:00 – 22:00";
        }

        // Tìm xem đã có phân ca tại vị trí + ca này chưa
        Optional<ShiftAssignment> existingOpt = shiftAssignmentRepository
                .findByHotel_IdAndWorkDateAndRoleAndShiftType(hotelId, request.getWorkDate(), request.getRole(), request.getShiftType());

        ShiftAssignment shift;
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

        List<ShiftAssignmentResponse> results = new ArrayList<>();
        for (ShiftAssignRequest item : request.getAssignments()) {
            results.add(assignShift(item, hotelId));
        }
        return results;
    }

    @Override
    @Transactional
    public void deleteShift(String shiftId) {
        if (shiftAssignmentRepository.existsById(shiftId)) {
            shiftAssignmentRepository.deleteById(shiftId);
        }
    }

    @Override
    @Transactional
    public WeeklyScheduleResponse initDefaultWeeklyScheduleIfEmpty(Long hotelId, LocalDate weekStartDate) {
        LocalDate monday = (weekStartDate != null)
                ? weekStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        List<ShiftAssignment> existing = shiftAssignmentRepository.findByHotelIdAndWorkDateBetween(hotelId, monday, sunday);
        if (!existing.isEmpty()) {
            return getWeeklySchedule(hotelId, monday);
        }

        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
        if (hotel == null) {
            return null;
        }

        List<Employee> staffList = employeeRepository.findByHotelId(hotelId);
        List<Employee> receptionists = staffList.stream()
                .filter(e -> e.getPosition() != null && e.getPosition().toLowerCase().contains("lễ tân"))
                .collect(Collectors.toList());
        List<Employee> housekeepings = staffList.stream()
                .filter(e -> e.getPosition() != null && (e.getPosition().toLowerCase().contains("buồng") || e.getPosition().toLowerCase().contains("housekeeping")))
                .collect(Collectors.toList());

        // Nếu danh sách chuyên môn trống thì chia đều nhân viên hiện có
        if (receptionists.isEmpty() && !staffList.isEmpty()) {
            receptionists.addAll(staffList.subList(0, Math.min(2, staffList.size())));
        }
        if (housekeepings.isEmpty() && staffList.size() > 2) {
            housekeepings.addAll(staffList.subList(2, staffList.size()));
        } else if (housekeepings.isEmpty() && !staffList.isEmpty()) {
            housekeepings.addAll(staffList);
        }

        List<ShiftAssignment> defaultAssignments = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);

            Employee recMorning = !receptionists.isEmpty() ? receptionists.get(i % receptionists.size()) : null;
            Employee recEvening = !receptionists.isEmpty() ? receptionists.get((i + 1) % receptionists.size()) : null;
            Employee hkMorning = !housekeepings.isEmpty() ? housekeepings.get(i % housekeepings.size()) : null;
            Employee hkEvening = !housekeepings.isEmpty() ? housekeepings.get((i + 1) % housekeepings.size()) : null;

            defaultAssignments.add(ShiftAssignment.builder()
                    .hotel(hotel).employee(recMorning).workDate(day)
                    .shiftType("Ca sáng").shiftTime("06:00 – 14:00")
                    .role("Lễ tân").task("Trực quầy lễ tân").status("SCHEDULED").build());

            defaultAssignments.add(ShiftAssignment.builder()
                    .hotel(hotel).employee(recEvening).workDate(day)
                    .shiftType("Ca tối").shiftTime("14:00 – 22:00")
                    .role("Lễ tân").task("Trực quầy lễ tân").status("SCHEDULED").build());

            defaultAssignments.add(ShiftAssignment.builder()
                    .hotel(hotel).employee(hkMorning).workDate(day)
                    .shiftType("Ca sáng").shiftTime("06:00 – 14:00")
                    .role("Housekeeping").task("Dọn phòng theo tầng").status("SCHEDULED").build());

            defaultAssignments.add(ShiftAssignment.builder()
                    .hotel(hotel).employee(hkEvening).workDate(day)
                    .shiftType("Ca tối").shiftTime("14:00 – 22:00")
                    .role("Housekeeping").task("Kiểm tra phòng cuối ngày").status("SCHEDULED").build());
        }

        shiftAssignmentRepository.saveAll(defaultAssignments);
        log.info("[ShiftService] Đã khởi tạo lịch phân ca mẫu cho tuần từ {} đến {} (khách sạn ID={})", monday, sunday, hotelId);

        return getWeeklySchedule(hotelId, monday);
    }

    private DailyShiftSummaryResponse buildDailySummary(LocalDate date, List<ShiftAssignmentResponse> assignments) {
        boolean fullyStaffed = assignments.size() >= 4 && assignments.stream().allMatch(a -> a.getEmployeeId() != null);
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
