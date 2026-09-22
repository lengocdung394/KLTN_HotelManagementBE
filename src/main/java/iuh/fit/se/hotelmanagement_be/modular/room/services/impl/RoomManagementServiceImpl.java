package iuh.fit.se.hotelmanagement_be.modular.room.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomSeasonalRate;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomSeasonalRateRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.services.RoomManagementService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomManagementServiceImpl implements RoomManagementService {
    RoomRepository roomRepository;
    BranchRoomPolicyRepository branchRoomPolicyRepository;
    RoomSeasonalRateRepository seasonalRateRepository;

    /**
     * Lấy giá tiền theo ngày cho TẤT CẢ CÁC PHÒNG trong một chi nhánh.
     * Cấu trúc trả về: Map<String roomId, Map<LocalDate ngày, Double giá>>
     */
    @Override
    public Map<String, Map<LocalDate, Double>> getAllRoomsDailyPricesByBranch(Long hotelId, LocalDate startDate, LocalDate endDate) {
        // 1. Lấy tất cả các phòng thuộc chi nhánh này
        // (Tùy theo cấu trúc entity Room của bạn, giả sử Room liên kết với Floor -> Branch qua hotelId)
        List<Room> rooms = roomRepository.findByFloor_Building_Hotel_Id(hotelId);
        // Hoặc nếu repo của bạn là findByBranchId(hotelId) thì thay vào đây.

        // 2. Lấy toàn bộ chính sách giá gốc theo loại phòng của chi nhánh
        Map<RoomType, Double> basePricesMap = new EnumMap<>(RoomType.class);
        for (RoomType type : RoomType.values()) {
            BranchRoomPolicy policy = branchRoomPolicyRepository.findByHotelIdAndRoomType(hotelId, type);
            double basePrice = (policy != null && policy.getBasePrice() != null) ? policy.getBasePrice() : 500000.0;
            basePricesMap.put(type, basePrice);
        }

        // 3. Lấy tất cả khung giá theo mùa trong khoảng thời gian của chi nhánh
        List<RoomSeasonalRate> seasonalRates = seasonalRateRepository.findRatesByBranchAndDateRange(hotelId, startDate, endDate);

        // 4. Xây dựng bảng giá chi tiết cho từng phòng cụ thể
        Map<String, Map<LocalDate, Double>> allRoomsPrices = new LinkedHashMap<>();

        for (Room room : rooms) {
            String roomId = room.getId(); // Ví dụ: "ROOM_20260922_A9F2B1"
            RoomType roomType = room.getRoomType();

            // Lấy giá cơ bản của loại phòng + tổng tiền tiện nghi riêng của phòng này
            double roomBasePrice = basePricesMap.getOrDefault(roomType, 500000.0);
            double amenitiesPrice = room.getTotalAmenitiesPrice(); // Hàm tính tổng tiền tiện nghi bạn đã có
            double standardDailyPrice = roomBasePrice + amenitiesPrice;

            // Khởi tạo dải ngày và lấp đầy bằng giá tiêu chuẩn của phòng này
            Map<LocalDate, Double> dailyPrices = new LinkedHashMap<>();
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                dailyPrices.put(current, standardDailyPrice);
                current = current.plusDays(1);
            }

            // Ghi đè bằng giá theo mùa nếu trùng loại phòng
            for (RoomSeasonalRate rate : seasonalRates) {
                if (rate.getRoomType().equals(roomType)) {
                    LocalDate rateStart = rate.getStartDate().isBefore(startDate) ? startDate : rate.getStartDate();
                    LocalDate rateEnd = rate.getEndDate().isAfter(endDate) ? endDate : rate.getEndDate();

                    LocalDate loopDate = rateStart;
                    while (!loopDate.isAfter(rateEnd)) {
                        // Giá mùa cũng cộng thêm tiện nghi riêng của phòng
                        double seasonalPrice = rate.getPrice() + amenitiesPrice;
                        dailyPrices.put(loopDate, seasonalPrice);
                        loopDate = loopDate.plusDays(1);
                    }
                }
            }

            allRoomsPrices.put(roomId, dailyPrices);
        }

        return allRoomsPrices;
    }
}

