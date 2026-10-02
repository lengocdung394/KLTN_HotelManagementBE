package iuh.fit.se.hotelmanagement_be.modular.booking.requests;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingModificationRequest {
    String employeeId;
    // 1. Phần HỦY PHÒNG & HỦY DỊCH VỤ LẺ
    List<String> bookingDetailIdsToCancel; // Các phòng muốn hủy

    List<ServiceCancellationRequest> servicesToCancel;  // Hủy dịch vụ lẻ (đã gom theo từng phòng)

    // 2. Phần THÊM MỚI PHÒNG (Đã bao gồm dịch vụ đi kèm cho phòng đó nếu có)
    List<NewRoomRequest> roomsToAdd;

    // 3. Phần ĐỔI PHÒNG
    List<RoomUpdateRequest> roomsToChange;

    // 5.PHẦN THÊM DỊCH VỤ PHÁT SINH cho phòng đang ở sẵn (Phòng cũ)
    List<RoomServiceAdditionRequest> servicesToAddForExistingRooms;

    //6. THÊM TRƯỜNG NÀY ĐỂ NHẬN YÊU CẦU CẬP NHẬT/GIẢM SỐ LƯỢNG DỊCH VỤ THEO PHÒNG
    List<UpdateServiceQuantityRequest> serviceQuantityUpdates;

    // 7. PHAN CHECK LAI KHUYEN MAI  CUA CHI NHANH- HOAC CUA CHINH KHACH HANG
    PromotionRequest promotionRequest;
    CustomerPromotionRequest customerPromotionRequest;


}
