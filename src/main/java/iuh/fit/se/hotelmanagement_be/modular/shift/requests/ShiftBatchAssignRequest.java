package iuh.fit.se.hotelmanagement_be.modular.shift.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftBatchAssignRequest {

    @NotEmpty(message = "Danh sách phân ca không được để trống")
    @Schema(description = "Danh sách phân ca làm việc cần lưu")
    List<@Valid ShiftAssignRequest> assignments;
}
