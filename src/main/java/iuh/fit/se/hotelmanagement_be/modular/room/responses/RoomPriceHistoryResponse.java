package iuh.fit.se.hotelmanagement_be.modular.room.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomPriceHistoryResponse {

    Long id;
    Long seasonalRateId;
    String rateName;         // Tên đợt giá để dễ nhìn trên UI
    Double oldPrice;
    Double newPrice;
    LocalDateTime changedAt;
    String changedByAdminName; // Tên admin đã thực hiện đổi giá
}
