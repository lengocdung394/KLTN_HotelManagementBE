package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingDetailResponseForHotel {
    Long bookingDetailId;
    Long roomId;
    String roomName;
    String roomTypeName;
    LocalDateTime checkInTime;
    LocalDateTime checkOutTime;
    int numAdults;
    int numChildren;
    int numInfants;

    Double baseRoomPricePerNight;       // Giá phòng gốc mỗi đêm (chưa phụ thu)
    Double extraAdultFeePerNight;       // Tiền phụ thu người lớn mỗi đêm
    Double extraChildFeePerNight;       // Tiền phụ thu trẻ em mỗi đêm
    Double roomSubTotal;                // Tổng tiền phòng (đã nhân số đêm + phụ thu)
    Double serviceSubTotal;             // Tổng tiền dịch vụ của phòng này
    Double totalPrice;
    List<BookingServiceResponseForHotel> bookingServiceResponsForHotels;// Tổng cộng cuối cùng của chi tiết này (Phòng + Dịch vụ)
}
