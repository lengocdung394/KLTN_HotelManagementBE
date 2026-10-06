package iuh.fit.se.hotelmanagement_be.modular.branch.responses;

import iuh.fit.se.hotelmanagement_be.modular.auth.responses.EmployeeResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponseForHotel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SuperAdminBranchDetailResponse {
    SuperAdminBranchSummaryResponse branch;
    List<EmployeeResponse> employees;
    List<BookingResponseForHotel> bookings;
    List<BuildingItem> buildings;
    List<FloorItem> floors;
    List<RoomItem> rooms;
    List<ServiceItem> services;
    List<PromotionItem> promotions;
    List<RoomPolicyItem> roomPolicies;

    public record BuildingItem(String id, String name, int floorCount) {
    }
    // <-- THÊM RECORD NÀY VÀO ĐỂ KHỚP VỚI HÀM toRoomPolicyItem CỦA BẠN
    public record RoomPolicyItem(
            String id,
            String roomType,
            Double area,              // Hoặc kiểu dữ liệu thực tế của bạn (ví dụ: float, double)
            Double basePrice,     // Hoặc Double tùy vào entity BranchRoomPolicy
            Double extraAdultFee,
            Double extraChildFee,
            int standardCapacity,
            int maxExtraGuests
    ) {
    }
    public record FloorItem(String id, int floorNumber, String buildingId, String buildingName, int roomCount) {
    }

    public record RoomItem(
            String id,
            String roomNumber,
            String roomType,
            String roomStatus,
            String floorId,
            int floorNumber,
            String buildingName) {
    }

    public record ServiceItem(String id, String name, String category, Double price, String unit, boolean shared) {
    }

    public record PromotionItem(String id, String code, String name, String status, String startDate, String endDate) {
    }
}
