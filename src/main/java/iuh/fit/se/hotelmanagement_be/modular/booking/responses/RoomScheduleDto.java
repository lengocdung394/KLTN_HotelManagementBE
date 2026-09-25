package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomScheduleDto {
    String bookingId;
    String customerName;
    LocalDateTime checkinTime;
    LocalDateTime checkoutTime;
    BookingStatus bookingStatus; // PENDING, CONFIRMED, IN_HOUSE, ...

    // Danh sách từng ngày cụ thể bị chiếm dụng trong khoảng checkin -> checkout
    List<LocalDate> occupiedDates;
}
