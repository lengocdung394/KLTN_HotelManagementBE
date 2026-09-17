package iuh.fit.se.hotelmanagement_be.modular.auth.responses;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.enums.LoyaltyTier;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerGetOneResponse {
    Long id;
    String fullName;
    String phone;
    String email;
    String cccd;
    LoyaltyTier loyaltyTier;      // Ví dụ: BRONZE, SILVER, GOLD, PLATINUM
    BigDecimal totalSpent;            // Tổng tiền đã chi tiêu
    int totalBookings;            // Tổng số lần đặt phòng
}
