package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import jakarta.persistence.Column;
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
    String roomId;
    String roomNumber;
    String roomName;
    String roomTypeName;
    LocalDateTime checkInTime;
    LocalDateTime checkOutTime;
    int numAdults;
    int numChildren;
    int numInfants;
    //
    // MỚI THÊM: Lưu lại chính xác thời điểm phòng này bị bấm hủy
    @Column(name = "cancelled_at")
    LocalDateTime cancelledAt;
   BookingStatusType bookingStatusType;
    // MỚI THÊM: Ai là người thực hiện hủy phòng này (Nhân viên nào)
    @Column(name = "cancelled_by")
    String cancelledBy;

    Double baseRoomPricePerNight;       // Giá phòng gốc mỗi đêm (chưa phụ thu)
    Double extraAdultFeePerNight;       // Tiền phụ thu người lớn mỗi đêm
    Double extraChildFeePerNight;       // Tiền phụ thu trẻ em mỗi đêm
    Double roomSubTotal;                // Tổng tiền phòng (đã nhân số đêm + phụ thu)
    Double serviceSubTotal;             // Tổng tiền dịch vụ của phòng này
    Double totalPrice;
    List<BookingServiceResponseForHotel> bookingServiceResponsForHotels;// Tổng cộng cuối cùng của chi tiết này (Phòng + Dịch vụ)
}
