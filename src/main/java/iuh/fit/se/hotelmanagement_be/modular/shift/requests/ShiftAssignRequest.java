package iuh.fit.se.hotelmanagement_be.modular.shift.requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftAssignRequest {

    @NotNull(message = "Ngày làm việc không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Ngày làm việc (YYYY-MM-DD)", example = "2026-10-14")
    LocalDate workDate;

    @Schema(description = "ID khách sạn chi nhánh", example = "1")
    Long hotelId;

    @NotBlank(message = "Loại ca không được để trống")
    @Schema(description = "Loại ca (Ca sáng / Ca tối / Ca đêm)", example = "Ca sáng")
    String shiftType;

    @Schema(description = "Giờ ca trực (mặc định: Ca sáng 06:00 – 14:00, Ca tối 14:00 – 22:00)", example = "06:00 – 14:00")
    String shiftTime;

    @NotBlank(message = "Vị trí không được để trống")
    @Schema(description = "Vị trí chuyên môn (Lễ tân / Housekeeping)", example = "Lễ tân")
    String role;

    @Schema(description = "Mã nhân viên được phân công", example = "EMP202609228492")
    String employeeId;

    @Schema(description = "Họ tên nhân viên được phân công", example = "Phạm Ngọc Anh")
    String employeeName;

    @Schema(description = "Nhiệm vụ cụ thể trong ca", example = "Trực quầy lễ tân")
    String task;

    @Schema(description = "Ghi chú thêm", example = "Ca trực chính")
    String note;
}
