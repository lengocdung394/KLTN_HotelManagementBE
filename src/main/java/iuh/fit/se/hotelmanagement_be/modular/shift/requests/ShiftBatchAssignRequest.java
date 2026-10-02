package iuh.fit.se.hotelmanagement_be.modular.shift.requests;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftBatchAssignRequest {

    @Schema(description = "ID khách sạn chi nhánh")
    Long hotelId;

    @NotEmpty(message = "Danh sách phân ca không được để trống")
    @JsonAlias({"shifts", "assignments"})
    @Schema(description = "Danh sách phân ca làm việc cần lưu")
    List<@Valid ShiftAssignRequest> assignments;
}
