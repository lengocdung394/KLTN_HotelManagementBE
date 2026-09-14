package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingDetailCreateRequest {
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
    double baseRoomPricePerNight;       // Giá phòng gốc mỗi đêm (chưa phụ thu)
    double extraAdultFeePerNight;       // Tiền phụ thu người lớn mỗi đêm
    double extraChildFeePerNight;       // Tiền phụ thu trẻ em mỗi đêm
    double roomSubTotal;                // Tổng tiền phòng (đã nhân số đêm + phụ thu)
    double serviceSubTotal;             // Tổng tiền dịch vụ của phòng này
    double totalPrice;                  // Tổng cộng cuối cùng của chi tiết này (Phòng + Dịch vụ)
    List<BookingServiceRequest> serviceRequests;
}
