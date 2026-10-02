package iuh.fit.se.hotelmanagement_be.modular.shift.responses;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WeeklyScheduleResponse {

    Long hotelId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate weekStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate weekEndDate;

    int daysFullyStaffed;     // ví dụ: 7/7
    int totalAssignments;     // ví dụ: 28 ca trong tuần
    int totalUniqueStaff;     // số nhân viên luân phiên tham gia trực
    List<DailyShiftSummaryResponse> days;
}
