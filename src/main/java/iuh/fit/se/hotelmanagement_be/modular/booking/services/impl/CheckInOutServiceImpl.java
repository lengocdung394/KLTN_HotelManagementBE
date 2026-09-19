package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingServiceDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.SurchargeCalculator;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.CheckInOutRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingDetailResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingService;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.CheckInOutService;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomSeasonalRate;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomSeasonalRateRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CheckInOutServiceImpl implements CheckInOutService {
    CheckInOutRepository checkInOutRepository;

    RoomSeasonalRateRepository roomSeasonalRateRepository;
    BookingRepository bookingRepository;
    BranchRoomPolicyRepository branchRoomPolicyRepository;
    BookingService bookingService;

    // Lay danh sach checkin
    @Override
    public List<BookingDetailResponse> getTodayCheckInList(Long hotelId) {
        LocalDate today = LocalDate.now();
        // Lọc ra các phòng có lịch check-in hôm nay và trạng thái vẫn đang chờ (PENDING)
        List<BookingDetail> checkInDetails = checkInOutRepository
                .findArrivalsByHotelAndDateAndStatus(hotelId, today, BookingStatusType.PENDING);

        return mapToBookingDetailResponseList(checkInDetails);
    }

    // Lay danh sach checkout
    @Override
    public List<BookingDetailResponse> getTodayCheckOutList(Long hotelId) {
        LocalDate today = LocalDate.now();
        // Lọc ra các phòng có lịch trả phòng hôm nay và khách đang ở trong phòng (CHECKED_IN)
        List<BookingDetail> checkOutDetails = checkInOutRepository
                .findDeparturesByHotelAndDateAndStatus(hotelId, today, BookingStatusType.CHECKED_IN);

        return mapToBookingDetailResponseList(checkOutDetails);
    }

    @Override
    public List<BookingDetailResponse> mapToBookingDetailResponseList(List<BookingDetail> details) {
        return details.stream().map(detail ->
                BookingDetailResponse.builder()
                        .bookingDetailId(detail.getId())
                        .roomId(detail.getRoom() != null ? detail.getRoom().getId() : null)
                        .roomName(detail.getRoom() != null ? detail.getRoom().getRoomType().toString() : null)
                        .roomTypeName(detail.getRoom() != null ? detail.getRoom().getRoomType().name() : null)
                        .checkInTime(detail.getCheckinTime())
                        .checkOutTime(detail.getCheckoutTime())
                        .numAdults(detail.getNumAdults())
                        .numChildren(detail.getNumChildren())
                        .baseRoomPricePerNight(detail.getBaseRoomPricePerNight())
                        .extraAdultFeePerNight(detail.getExtraAdultFeePerNight())
                        .extraChildFeePerNight(detail.getExtraChildFeePerNight())
                        .roomSubTotal(detail.getRoomSubTotal())
                        .serviceSubTotal(detail.getServiceSubTotal())
                        .totalPrice(detail.getTotalPrice())
                        .build()
        ).toList();
    }

    // Phan checkin checkout

    /**
     * HELPER METHOD: Lấy chính xác giá phòng của một ngày cụ thể dựa trên câu Query của bạn [2]
     */
    private BigDecimal getDailyRoomPrice(Room room, LocalDate date, Long hotelId, BranchRoomPolicy policy) {
        // 💡 SỬ DỤNG HÀM CỦA BẠN: Tìm giá mùa vụ khớp với ngày được truyền vào [2]
        Optional<RoomSeasonalRate> seasonalRateOpt = roomSeasonalRateRepository
                .findActiveRateByDate(hotelId, room.getRoomType(), date);

        double dailyRoomPrice;
        double amenitiesPrice = room.getTotalAmenitiesPrice(); // Tiền dịch vụ tiện nghi cố định của phòng [2]

        if (seasonalRateOpt.isPresent()) {
            // Nếu ngày đó nằm trong khung mùa vụ -> Lấy giá mùa vụ + tiền tiện ích [2]
            dailyRoomPrice = seasonalRateOpt.get().getPrice() + amenitiesPrice;
        } else {
            // Nếu là ngày thường -> Lấy giá nền của chính sách + tiền tiện ích [2]
            double basePrice = policy.getBasePrice() != null ? policy.getBasePrice() : 0.0;
            dailyRoomPrice = basePrice + amenitiesPrice;
        }

        return BigDecimal.valueOf(dailyRoomPrice);
    }


    /**
     * 1. NGHIỆP VỤ CHECK-IN HÀNG LOẠT (Quét giá mùa vụ của ngày hôm nay để tính phạt đến sớm) [2]
     */
    @Transactional
    @Override
    public BookingResponse processBulkCheckIn(Long bookingId, List<Long> bookingDetailIds, Long employeeId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        Order order = booking.getOrder();
        BigDecimal batchEarlyFeeTotal = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        LocalDate todayDate = now.toLocalDate(); // Lấy ngày hôm nay làm mốc quét giá mùa vụ [2]

        for (Long detailId : bookingDetailIds) {
            BookingDetail targetDetail = booking.getBookingDetails().stream()
                    .filter(d -> d.getId().equals(detailId))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND));

            if (targetDetail.getStatus() != BookingStatusType.PENDING) {
                continue;
            }

            targetDetail.setStatus(BookingStatusType.CHECKED_IN);
            targetDetail.setActualCheckInTime(now);

            // Trích xuất thông tin cấu hình từ thực thể phòng của khách [2]
            Room room = targetDetail.getRoom();
            Long hotelId = room.getFloor().getBuilding().getHotel().getId();
            BranchRoomPolicy policy = branchRoomPolicyRepository.findByHotelIdAndRoomType(hotelId, room.getRoomType());

            if (policy == null) {
                throw new AppException(ErrorCode.BRANCH_POLICY_NOT_FOUND);
            }

            // Gọi hàm Helper ứng dụng câu lệnh Query của bạn để lấy đúng giá phòng hôm nay [2]
            BigDecimal actualTodayRoomPrice = getDailyRoomPrice(room, todayDate, hotelId, policy);

            // Đưa giá thực tế ngày hôm nay vào bộ tính phụ thu của bạn
            BigDecimal earlyFee = SurchargeCalculator.calculateEarlyCheckInFee(
                    targetDetail.getCheckinTime(),
                    targetDetail.getActualCheckInTime(),
                    actualTodayRoomPrice
            );

            if (earlyFee.compareTo(BigDecimal.ZERO) > 0) {
                targetDetail.setEarlyCheckInFee(earlyFee);
                batchEarlyFeeTotal = batchEarlyFeeTotal.add(earlyFee);
            }
        }

        if (batchEarlyFeeTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentSurchargeTotal = order.getSurchargeTotalAmount() != null ? order.getSurchargeTotalAmount() : BigDecimal.ZERO;
            order.setSurchargeTotalAmount(currentSurchargeTotal.add(batchEarlyFeeTotal));
            order.setOrderStatus(OrderStatusType.OPEN);
        }

        Booking saved = bookingRepository.save(booking);
        return bookingService.toBookingResponse(saved);
    }

    /**
     * 2. NGHIỆP VỤ CHECK-OUT HÀNG LOẠT (Quét giá mùa vụ của ngày hôm nay để tính phạt lố giờ) [2]
     */
    @Transactional
    @Override
    public BookingResponse processBulkCheckOut(Long bookingId, List<Long> bookingDetailIds, Long employeeId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        Order order = booking.getOrder();
        BigDecimal batchLateFeeTotal = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        LocalDate todayDate = now.toLocalDate(); // Lấy ngày hôm nay làm mốc quét giá mùa vụ [2]

        for (Long detailId : bookingDetailIds) {
            BookingDetail targetDetail = booking.getBookingDetails().stream()
                    .filter(d -> d.getId().equals(detailId))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND));
            if (targetDetail.getStatus() != BookingStatusType.CHECKED_IN) {
                continue;
            }

            targetDetail.setStatus(BookingStatusType.CHECKED_OUT);
            targetDetail.setActualCheckOutTime(now);

            Room room = targetDetail.getRoom();
            Long hotelId = room.getFloor().getBuilding().getHotel().getId();
            BranchRoomPolicy policy = branchRoomPolicyRepository.findByHotelIdAndRoomType(hotelId, room.getRoomType());

            if (policy == null) {
                throw new AppException(ErrorCode.BRANCH_POLICY_NOT_FOUND);
            }

            // Gọi hàm Helper ứng dụng câu lệnh Query của bạn để lấy đúng giá phòng hôm nay [2]
            BigDecimal actualTodayRoomPrice = getDailyRoomPrice(room, todayDate, hotelId, policy);

            // Đưa giá thực tế ngày hôm nay vào bộ tính phụ thu của bạn
            BigDecimal lateFee = SurchargeCalculator.calculateLateCheckOutFee(
                    targetDetail.getCheckoutTime(),
                    targetDetail.getActualCheckOutTime(),
                    actualTodayRoomPrice
            );

            if (lateFee.compareTo(BigDecimal.ZERO) > 0) {
                targetDetail.setLateCheckOutFee(lateFee);
                batchLateFeeTotal = batchLateFeeTotal.add(lateFee);
            }

            // Khóa sổ dịch vụ phòng
            if (targetDetail.getBookingServiceDetails() != null) {
                for (BookingServiceDetail sd : targetDetail.getBookingServiceDetails()) {
                    if (!Boolean.TRUE.equals(sd.getIsPaid()) && !Boolean.TRUE.equals(sd.getCancelled())) {
                        sd.setIsPaid(true);
                        sd.setPaidAt(now);
                    }
                }
            }
        }

        if (batchLateFeeTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentSurchargeTotal = order.getSurchargeTotalAmount() != null ? order.getSurchargeTotalAmount() : BigDecimal.ZERO;
            order.setSurchargeTotalAmount(currentSurchargeTotal.add(batchLateFeeTotal));
        }

        order.setPaidAmount(order.getTotalAmount());
        order.operation();

        Booking saved = bookingRepository.save(booking);
        return bookingService.toBookingResponse(saved);
    }


}
