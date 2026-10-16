package iuh.fit.se.hotelmanagement_be.modular.branch.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BuildingCreateRequest {
    @NotBlank(message = "Tên tòa nhà không được để trống")
    String name;
}
