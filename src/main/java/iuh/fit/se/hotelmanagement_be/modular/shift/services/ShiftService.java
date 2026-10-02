package iuh.fit.se.hotelmanagement_be.modular.shift.services;

import iuh.fit.se.hotelmanagement_be.modular.shift.requests.ShiftAssignRequest;
import iuh.fit.se.hotelmanagement_be.modular.shift.requests.ShiftBatchAssignRequest;
import iuh.fit.se.hotelmanagement_be.modular.shift.responses.DailyShiftSummaryResponse;
import iuh.fit.se.hotelmanagement_be.modular.shift.responses.ShiftAssignmentResponse;
import iuh.fit.se.hotelmanagement_be.modular.shift.responses.WeeklyScheduleResponse;

import java.time.LocalDate;
import java.util.List;

public interface ShiftService {

    List<ShiftAssignmentResponse> getTodayShifts(Long hotelId, LocalDate date);

    DailyShiftSummaryResponse getDailyShiftSummary(Long hotelId, LocalDate date);

    WeeklyScheduleResponse getWeeklySchedule(Long hotelId, LocalDate weekStartDate);

    ShiftAssignmentResponse assignShift(ShiftAssignRequest request, Long hotelId);

    List<ShiftAssignmentResponse> assignBatchShifts(ShiftBatchAssignRequest request, Long hotelId);

    void deleteShift(String shiftId);

    WeeklyScheduleResponse initDefaultWeeklyScheduleIfEmpty(Long hotelId, LocalDate weekStartDate);
}
