package iuh.fit.se.hotelmanagement_be.modular.room.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomBedRequest {
    @NotNull(message = "Loại giường không được để trống.")
    Long bedTypeId;


    @NotNull(message = "Số lượng giường không được để trống.")
    @Min(value = 1, message = "Số lượng giường tối thiểu là 1.")
    Integer quantity;
}
