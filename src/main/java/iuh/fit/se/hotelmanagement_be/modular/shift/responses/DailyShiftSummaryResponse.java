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
public class DailyShiftSummaryResponse {

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate date;

    String dayKey;        // "mon", "tue", ...
    String dayLabel;      // "Thứ 2", "Thứ 3", ...
    String dateDisplay;   // "14/10"
    boolean fullyStaffed; // Đủ cả 4 ca (2 lễ tân + 2 housekeeping)
    int totalAssigned;
    List<ShiftAssignmentResponse> assignments;
}
