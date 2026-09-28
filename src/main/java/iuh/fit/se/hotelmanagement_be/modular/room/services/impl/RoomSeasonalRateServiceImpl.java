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
import iuh.fit.se.hotelmanagement_be.modular.room.requests.RoomSeasonalRateUpdateRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomPriceHistoryResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomSeasonalRateResponse;
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
    public List<RoomSeasonalRateResponse> saveOrUpdateBatchSeasonalRates(List<RoomSeasonalRateUpdateRequest> requests, Account currentAdmin) {
        List<RoomSeasonalRateResponse> responses = new ArrayList<>();

        for (RoomSeasonalRateUpdateRequest request : requests) {
            RoomSeasonalRate rate;
            // ĐÚNG: Kiểm tra id != null trước, nếu khác null mới gọi DB kiểm tra tồn tại
            boolean isUpdate = (request.getId() != null) && roomSeasonalRateRepository.existsById(request.getId());
            if (isUpdate) {
                // ==================== TRƯỜNG HỢP 1: CẬP NHẬT (UPDATE) ====================
                rate = roomSeasonalRateRepository.findById(request.getId()).get();

                // Kiểm tra trùng lặp thời gian (loại trừ chính ID đang sửa)
                boolean isOverlap = roomSeasonalRateRepository.existsOverlappingRateExcludingId(
                        currentAdmin.getHotelId(),
                        rate.getRoomType(),
                        request.getStartDate(),
                        request.getEndDate(),
                        request.getId()
                );

                if (isOverlap) {
                    throw new AppException(ErrorCode.DUPLICATE_SEASONAL_RATE);
                }

                double oldPrice = rate.getPrice();
                double newPrice = request.getPrice();

                // Cập nhật thông tin
                rate.setRateName(request.getRateName());
                rate.setStartDate(request.getStartDate());
                rate.setEndDate(request.getEndDate());
                rate.setPrice(newPrice);

                RoomSeasonalRate savedRate = roomSeasonalRateRepository.save(rate);

                // Chỉ lưu lịch sử nếu giá tiền thực sự thay đổi
                if (oldPrice != newPrice) {
                    RoomPriceHistory history = RoomPriceHistory.builder()
                            .seasonalRate(savedRate)
                            .account(currentAdmin)
                            .oldPrice(oldPrice)
                            .newPrice(newPrice)
                            .changedAt(LocalDateTime.now())
                            .build();
                    roomPriceHistoryRepository.save(history);
                }

                responses.add(mapToResponse(savedRate));

            } else {
                // ==================== TRƯỜNG HỢP 2: THÊM MỚI (CREATE - BỔ SUNG PHÒNG MỚI) ====================
                // Kiểm tra trùng lặp toàn bộ với DB cho loại phòng mới này
                boolean isOverlap = roomSeasonalRateRepository.existsOverlappingRate(
                        currentAdmin.getHotelId(),
                        request.getRoomType(),
                        request.getStartDate(),
                        request.getEndDate()
                );
                // kiem tra lai xem co trung voi cai cu hay khong
                if (isOverlap) {
                    throw new AppException(ErrorCode.DUPLICATE_SEASONAL_RATE);
                }

                // Tạo entity mới
                RoomSeasonalRate newRate = RoomSeasonalRate.builder()
                        .hotelId(currentAdmin.getHotelId())
                        .roomType(request.getRoomType())
                        .rateName(request.getRateName())
                        .startDate(request.getStartDate())
                        .endDate(request.getEndDate())
                        .price(request.getPrice())
                        .build();

                RoomSeasonalRate savedRate = roomSeasonalRateRepository.save(newRate);

                responses.add(mapToResponse(savedRate));
            }
        }

        // Bắn Socket thông báo realtime cho nhân viên chi nhánh
        if (!requests.isEmpty()) {
            roomSeasonalRateSocketEmitter.emitSeasonalRateUpdated(currentAdmin.getHotelId(), responses);
        }

        return responses;
    }

    @Override
    public Page<RoomSeasonalRate> getRatesByDate(Long hotelId, RoomType roomType, LocalDate date, Pageable pageable) {
        // Nếu không truyền ngày thì mặc định lấy ngày hôm nay
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        return roomSeasonalRateRepository.findActiveRatesByDate(hotelId, roomType, targetDate, pageable);
    }

    @Override
    public List<RoomSeasonalRateResponse> getRatesByMonth(Long hotelId, int month, int year) {
        // 1. Xác định ngày đầu tiên và ngày cuối cùng của tháng đó
        LocalDate startDateOfMonth = LocalDate.of(year, month, 1);
        LocalDate endDateOfMonth = startDateOfMonth.plusMonths(1).minusDays(1); // Hoặc dùng YearMonth.of(year, month).atEndOfMonth()

        // 2. Truy vấn Database theo chi nhánh và khoảng thời gian tháng
        List<RoomSeasonalRate> rates = roomSeasonalRateRepository.findRatesByMonth(hotelId, startDateOfMonth, endDateOfMonth);

        // 3. Map sang danh sách Response DTO
        return rates.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
