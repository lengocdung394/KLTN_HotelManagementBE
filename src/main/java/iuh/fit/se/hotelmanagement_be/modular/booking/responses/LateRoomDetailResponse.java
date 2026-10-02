package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import iuh.fit.se.hotelmanagement_be.modular.booking.responses.enums.SurchargeLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LateRoomDetailResponse {

    String bookingDetailId; // ID chi tiết phòng
    String roomNumber;      // Số phòng
    LocalDateTime scheduledCheckOut;
    LocalDateTime currentTime;
    BigDecimal currentSurcharge;
    SurchargeLevel surchargeLevel;
}
