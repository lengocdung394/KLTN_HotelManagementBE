package iuh.fit.se.hotelmanagement_be.modular.room.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomSeasonalRateUpdateRequest {
    @NotNull(message = "Giá phòng mới không được để trống")
    @DecimalMin(value = "0.0", message = "Giá phòng không được âm")
    Double newPrice;
}
