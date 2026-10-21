package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatbotBookingRequest {
    String customerName;
    String customerPhone;
    String customerEmail;
    Long hotelId;
    List<String> roomIds;
    LocalDateTime checkInTime;
    LocalDateTime checkOutTime;
    Integer numAdults;
    Integer numChildren;
    List<String> serviceIds;
    String promoCode;
    String paymentMethod;
}
