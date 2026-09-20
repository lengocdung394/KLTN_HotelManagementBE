package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewRoomRequest {


    @NotNull(message = "Mã phòng không được để trống")
    Long roomId;

    @NotNull(message = "Thời gian nhận phòng không được để trống")
    LocalDateTime checkInTime;

    @NotNull(message = "Thời gian trả phòng không được để trống")
    LocalDateTime checkOutTime;

    @NotNull(message = "Số người lớn không được để trống")
    Integer numAdults;

    @NotNull(message = "Số trẻ em không được để trống")
    Integer numChildren;

    @NotNull(message = "Số em bé không được để trống")
    Integer numInfants;
    // --- CÁC KHOẢN CHI TIẾT ---
//    Double baseRoomPricePerNight;       // Giá phòng gốc mỗi đêm (chưa phụ thu)
//    Double extraAdultFeePerNight;       // Tiền phụ thu người lớn mỗi đêm
//    Double extraChildFeePerNight;       // Tiền phụ thu trẻ em mỗi đêm
//    Double roomSubTotal;                // Tổng tiền phòng (đã nhân số đêm + phụ thu)
//    Double serviceSubTotal;             // Tổng tiền dịch vụ của phòng này
//    Double totalPrice;                  // Tổng cộng cuối cùng của chi tiết này (Phòng + Dịch vụ)
    List<NewServiceRequest> serviceRequests;
}
