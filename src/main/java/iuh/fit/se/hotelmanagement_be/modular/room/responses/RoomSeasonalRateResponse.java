package iuh.fit.se.hotelmanagement_be.modular.room.responses;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomSeasonalRateResponse {
    Long id;
    Long hotelId;
    RoomType roomType;
    String rateName;
    LocalDate startDate;
    LocalDate endDate;
    Double price;
}
