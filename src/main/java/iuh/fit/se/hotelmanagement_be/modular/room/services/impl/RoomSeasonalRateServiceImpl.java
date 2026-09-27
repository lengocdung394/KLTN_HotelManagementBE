package iuh.fit.se.hotelmanagement_be.modular.room.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomPriceHistory;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomSeasonalRate;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomPriceHistoryRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomSeasonalRateRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.RoomSeasonalRateCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomPriceHistoryResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomSeasonalRateResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomSeasonalRateSocketEmitter;
import iuh.fit.se.hotelmanagement_be.modular.room.services.RoomSeasonalRateService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomSeasonalRateServiceImpl implements RoomSeasonalRateService {
    RoomPriceHistoryRepository roomPriceHistoryRepository;
    BranchRoomPolicyRepository branchRoomPolicyRepository;
    RoomSeasonalRateRepository roomSeasonalRateRepository;
    RoomSeasonalRateSocketEmitter roomSeasonalRateSocketEmitter;

    // ham tao gia theo su kien
    @Transactional
    @Override
    public List<RoomSeasonalRateResponse> createSeasonalRate(List<RoomSeasonalRateCreateRequest> requests, Account currentAdmin) {
        List<RoomSeasonalRateResponse> responses = new ArrayList<>();

        for (RoomSeasonalRateCreateRequest request : requests) {
            // 1. Kiểm tra trùng lặp với dữ liệu đã có trong DB
            boolean isOverlap = roomSeasonalRateRepository.existsOverlappingRate(
                    currentAdmin.getHotelId(),
                    request.getRoomType(),
                    request.getStartDate(),
                    request.getEndDate()
            );

            if (isOverlap) {
                throw new AppException(ErrorCode.DUPLICATE_SEASONAL_RATE);
                // Hoặc có thể custom message kèm theo tên đợt giá bị trùng để dễ debug
            }

            // 2. Lấy giá gốc cơ bản
            double baseRoomPrice = branchRoomPolicyRepository
                    .findByHotelIdAndRoomType(currentAdmin.getHotelId(), request.getRoomType()).getBasePrice();

            // 3. Tạo entity
            RoomSeasonalRate newRate = RoomSeasonalRate.builder()
                    .hotelId(currentAdmin.getHotelId())
                    .roomType(request.getRoomType())
                    .rateName(request.getRateName())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .price(request.getPrice())
                    .build();

            RoomSeasonalRate savedRate = roomSeasonalRateRepository.save(newRate);

            // 4. Lưu lịch sử thay đổi giá
            RoomPriceHistory history = RoomPriceHistory.builder()
                    .seasonalRate(savedRate)
                    .account(currentAdmin)
                    .oldPrice(baseRoomPrice)
                    .newPrice(savedRate.getPrice())
                    .changedAt(LocalDateTime.now())
                    .build();
            roomPriceHistoryRepository.save(history);

            responses.add(mapToResponse(savedRate));
        }

        // 5. Bắn Socket một lần cho cả batch (Lấy hotelId từ request đầu tiên)
        if (!requests.isEmpty()) {

            roomSeasonalRateSocketEmitter.emitSeasonalRateCreated(currentAdmin.getHotelId(), responses);
        }

        return responses;
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

    @Override
    public Page<RoomSeasonalRate> getRatesByDate(Long hotelId, RoomType roomType, LocalDate date, Pageable pageable) {
        // Nếu không truyền ngày thì mặc định lấy ngày hôm nay
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        return roomSeasonalRateRepository.findActiveRatesByDate(hotelId, roomType, targetDate, pageable);
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

    // ham lay tat ca su kien


}
