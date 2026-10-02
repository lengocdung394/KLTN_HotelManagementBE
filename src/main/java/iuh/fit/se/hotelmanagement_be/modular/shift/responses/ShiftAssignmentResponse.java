package iuh.fit.se.hotelmanagement_be.modular.shift.responses;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftAssignmentResponse {

    String id;
    String employeeId;
    String employeeName;
    String employeePhone;
    String avatarUrl;
    String initials;
    String position;
    Long hotelId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate workDate;

    String dayKey;        // "mon", "tue", ...
    String dayLabel;      // "Thứ 2", "Thứ 3", ...
    String dateDisplay;   // "14/10"
    String shiftType;     // "Ca sáng", "Ca tối"
    String shiftTime;     // "06:00 – 14:00"
    String role;          // "Lễ tân", "Housekeeping"
    String task;          // "Trực quầy lễ tân"
    String status;        // "SCHEDULED"
    String note;
}
