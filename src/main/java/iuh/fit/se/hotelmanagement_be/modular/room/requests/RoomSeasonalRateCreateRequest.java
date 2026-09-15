package iuh.fit.se.hotelmanagement_be.modular.room.requests;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomSeasonalRateCreateRequest {
    @NotNull(message = "Chi nhánh không được để trống")
    Long hotelId;

    @NotNull(message = "Loại phòng không được để trống")
    RoomType roomType;

    @NotBlank(message = "Tên đợt giá không được để trống")
    String rateName;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @FutureOrPresent(message = "Ngày bắt đầu phải từ hôm nay trở đi")
    LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    LocalDate endDate;

    @NotNull(message = "Giá phòng không được để trống")
    @DecimalMin(value = "0.0", message = "Giá phòng không được âm")
    Double price;
}
