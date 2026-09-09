package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingResponse {
    Long bookingId;
    Long customerId;
    String customerName;
    BookingStatus bookingStatus;
    BookingChannel bookingChannel;
    LocalDateTime createdAt;
    List<BookingDetailResponse> bookingDetails;
}
