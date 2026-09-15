package iuh.fit.se.hotelmanagement_be.modular.room.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomPriceHistory;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomSeasonalRate;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomPriceHistoryRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomSeasonalRateRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.RoomSeasonalRateCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomPriceHistoryResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomSeasonalRateResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.services.RoomSeasonalRateService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomSeasonalRateServiceImpl implements RoomSeasonalRateService {
    RoomPriceHistoryRepository roomPriceHistoryRepository;
    BranchRoomPolicyRepository branchRoomPolicyRepository;
    RoomSeasonalRateRepository roomSeasonalRateRepository;

    @Transactional
    @Override
    public RoomSeasonalRateResponse createSeasonalRate(RoomSeasonalRateCreateRequest request, Account currentAdmin) {
        // Lấy giá gốc cơ bản làm mốc oldPrice
        double baseRoomPrice = branchRoomPolicyRepository
                .findByHotelIdAndRoomType(request.getHotelId(), request.getRoomType()).getBasePrice();

        RoomSeasonalRate newRate = RoomSeasonalRate.builder()
                .hotelId(request.getHotelId())
                .roomType(request.getRoomType())
                .rateName(request.getRateName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .price(request.getPrice())
                .build();

        RoomSeasonalRate savedRate = roomSeasonalRateRepository.save(newRate);

        // Ghi log lịch sử khởi tạo
        RoomPriceHistory history = RoomPriceHistory.builder()
                .seasonalRate(savedRate)
                .account(currentAdmin)
                .oldPrice(baseRoomPrice)
                .newPrice(savedRate.getPrice())
                .changedAt(LocalDateTime.now())
                .build();

        roomPriceHistoryRepository.save(history);

        return mapToResponse(savedRate);
    }

    @Transactional
    @Override
    public RoomSeasonalRateResponse updateSeasonalRatePrice(Long rateId, Double newPrice, Account currentAdmin) {
        RoomSeasonalRate rate = roomSeasonalRateRepository.findById(rateId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khung giá với ID: " + rateId));

        double oldPrice = rate.getPrice();

        if (oldPrice != newPrice) {
            rate.setPrice(newPrice);
            roomSeasonalRateRepository.save(rate);

            RoomPriceHistory history = RoomPriceHistory.builder()
                    .seasonalRate(rate)
                    .account(currentAdmin)
                    .oldPrice(oldPrice)
                    .newPrice(newPrice)
                    .changedAt(LocalDateTime.now())
                    .build();

            roomPriceHistoryRepository.save(history);
        }

        return mapToResponse(rate);
    }
    // 3. Xem lịch sử thay đổi giá của một khung giá
    public List<RoomPriceHistoryResponse> getPriceHistories(Long rateId) {
        RoomSeasonalRate rate = roomSeasonalRateRepository.findById(rateId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khung giá"));

        List<RoomPriceHistory> histories = roomPriceHistoryRepository.findBySeasonalRateOrderByChangedAtDesc(rate);

        return histories.stream().map(h -> RoomPriceHistoryResponse.builder()
                .id(h.getId())
                .seasonalRateId(rate.getId())
                .rateName(rate.getRateName())
                .oldPrice(h.getOldPrice())
                .newPrice(h.getNewPrice())
                .changedAt(h.getChangedAt())
                .changedByAdminName(h.getAccount() != null ? h.getAccount().getUsername() : "System")
                .build()
        ).collect(Collectors.toList());
    }

    private RoomSeasonalRateResponse mapToResponse(RoomSeasonalRate savedRate) {
        return RoomSeasonalRateResponse.builder()
                .id(savedRate.getId())
                .endDate(savedRate.getEndDate())
                .startDate(savedRate.getStartDate())
                .price(savedRate.getPrice())
                .rateName(savedRate.getRateName())
                .hotelId(savedRate.getHotelId())
                .roomType(savedRate.getRoomType()).build();
    }


}
