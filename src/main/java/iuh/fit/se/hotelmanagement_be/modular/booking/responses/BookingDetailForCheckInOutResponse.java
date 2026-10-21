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
public class BookingDetailForCheckInOutResponse {

    String cccd;
    String nameCustomer;
    String phone;
    String bookingId;
    String bookingDetailId;
    String roomId;
    String roomNumber;
    String roomName;
    String roomTypeName;
    LocalDateTime checkInTime;
    LocalDateTime checkOutTime;
    LocalDateTime actualCheckInTime;
    int numAdults;
    int numChildren;
    int numInfants;

    Double baseRoomPricePerNight;       // Giá phòng gốc mỗi đêm (chưa phụ thu)
    Double extraAdultFeePerNight;       // Tiền phụ thu người lớn mỗi đêm
    Double extraChildFeePerNight;       // Tiền phụ thu trẻ em mỗi đêm
    Double roomSubTotal;                // Tổng tiền phòng (đã nhân số đêm + phụ thu)
    Double serviceSubTotal;             // Tổng tiền dịch vụ của phòng này
    Double totalPrice;                  // Tổng cộng cuối cùng của chi tiết này (Phòng + Dịch vụ)
    Double earlyCheckInFee;             // Phụ phí check-in sớm dự kiến
    Double lateCheckOutFee;             // Phụ phí check-out trễ đã tính
    Double otherSurcharges;
    Double estimatedEarlyCheckInFee;    // Ước tính phụ phí nếu check-in ngay
    String paymentStatus;               // Trạng thái thanh toán (OPEN, CLOSED...)
    String bookingDetailStatus;

    Double paidAmount;                  // Số tiền đã thanh toán của booking
    Double remainingAmount; // so tien chua duoc thanh toan
    Double surplusAmount;               // Số tiền cần hoàn lại cho khách
    Boolean roomPaid;                   // Đã thanh toán tiền phòng chưa
    List<BookingServiceResponseForHotel>  bookingServiceResponseForHotel;
}
