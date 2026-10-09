package iuh.fit.se.hotelmanagement_be.modular.branch.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FloorCreateRequest {
    @NotBlank(message = "Tòa nhà không được để trống")
    String buildingId;

    @NotNull(message = "Số tầng không được để trống")
    Integer floorNumber;
}
