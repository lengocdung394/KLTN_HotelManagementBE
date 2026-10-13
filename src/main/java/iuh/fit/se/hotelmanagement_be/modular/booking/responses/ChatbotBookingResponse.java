package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatbotBookingResponse {
    String bookingId;
    String orderId;
    Long orderCode;
    String customerId;
    BigDecimal totalAmount;
    String roomId;
}
