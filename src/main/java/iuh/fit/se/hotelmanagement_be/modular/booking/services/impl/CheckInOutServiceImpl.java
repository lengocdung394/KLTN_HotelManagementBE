package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingServiceDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.SurchargeCalculator;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.CheckInOutRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingDetailForCheckInOutResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingServiceResponseForHotel;
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
import java.util.Collections;
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

    // 💡 LUỒNG XỬ LÝ LINH HOẠT CHO DANH SÁCH CHECK-IN
    @Override
    public List<BookingDetailForCheckInOutResponse> getTodayCheckInList(Long hotelId, LocalDate date, BookingStatusType status, BookingStatus bookingStatus) {
        // 1. Nếu Ngày truyền vào bị null -> Mặc định lấy ngày hôm nay
        LocalDate finalDate = (date != null) ? date : LocalDate.now();

        // 2. Nếu Trạng thái truyền vào bị null -> Mặc định lấy phòng đang chờ nhận (PENDING)
        BookingStatusType finalStatus = (status != null) ? status : BookingStatusType.PENDING;
        //
        BookingStatus finalBookingStatus = (bookingStatus != null) ? bookingStatus : BookingStatus.CONFIRMED;
        List<BookingDetail> checkInDetails = checkInOutRepository
                .findArrivalsByHotelAndDateAndStatus(hotelId, finalDate, finalBookingStatus, finalStatus);

        return mapToBookingDetailResponseList(checkInDetails);
    }

    // 💡 LUỒNG XỬ LÝ LINH HOẠT CHO DANH SÁCH CHECK-OUT
    @Override
    public List<BookingDetailForCheckInOutResponse> getTodayCheckOutList(Long hotelId, LocalDate date, BookingStatusType status, BookingStatus bookingStatus) {
        // 1. Nếu Ngày truyền vào bị null -> Mặc định lấy ngày hôm nay
        LocalDate finalDate = (date != null) ? date : LocalDate.now();

        // 2. Nếu Trạng thái truyền vào bị null -> Mặc định lấy phòng đang lưu trú (CHECKED_IN)
        BookingStatusType finalStatus = (status != null) ? status : BookingStatusType.CHECKED_IN;

        //
        BookingStatus finalBookingStatus = (bookingStatus != null) ? bookingStatus : BookingStatus.CONFIRMED;
        List<BookingDetail> checkOutDetails = checkInOutRepository
                .findDeparturesByHotelAndDateAndStatus(hotelId, finalDate, finalBookingStatus, finalStatus);

        return mapToBookingDetailResponseList(checkOutDetails);
    }

    @Override
    public List<BookingDetailForCheckInOutResponse> mapToBookingDetailResponseList(List<BookingDetail> details) {
        return details.stream().map(detail ->
                BookingDetailForCheckInOutResponse.builder()
                        .roomNumber(detail.getRoom().getRoomNumber())
                        // Thong tin khach hang
                        .cccd(detail.getBooking().getCustomer().getCccd())
                        .nameCustomer(detail.getBooking().getCustomer().getFullName())
                        .bookingId(detail.getBooking().getId())
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
                        // Moc noi phan dich vu
                        .bookingServiceResponseForHotel(detail.getBookingServiceDetails() != null ?
                                detail.getBookingServiceDetails().stream()
                                        .map(v -> BookingServiceResponseForHotel.builder()
                                                .serviceId(v.getService() != null ? v.getService().getId() : null)
                                                .price(v.getPrice())
                                                .name(v.getName())
                                                .quantity(v.getQuantity())
                                                .usedAt(v.getUsedAt()).build()).toList() : Collections.emptyList()
                        ).build()


        ).toList();
    }

    private BigDecimal getDailyRoomPrice(Room room, LocalDate date, Long hotelId, BranchRoomPolicy policy) {
        Optional<RoomSeasonalRate> seasonalRateOpt =
                roomSeasonalRateRepository.findActiveRateByDate(hotelId, room.getRoomType(), date);

        double amenitiesPrice = room.getTotalAmenitiesPrice() != null ? room.getTotalAmenitiesPrice() : 0.0;
        double dailyRoomPrice;

        if (seasonalRateOpt.isPresent()) {
            dailyRoomPrice = seasonalRateOpt.get().getPrice() + amenitiesPrice;
        } else {
            double basePrice = policy.getBasePrice() != null ? policy.getBasePrice() : 0.0;
            dailyRoomPrice = basePrice + amenitiesPrice;
        }

        return BigDecimal.valueOf(dailyRoomPrice);
    }
    @Transactional
    @Override
    public BookingResponse processBulkCheckIn(String bookingId, List<Long> bookingDetailIds, String employeeId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        Order order = booking.getOrder();
        BigDecimal batchEarlyFeeTotal = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        for (Long detailId : bookingDetailIds) {
            BookingDetail targetDetail = booking.getBookingDetails().stream()
                    .filter(d -> d.getId().equals(detailId))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND));

            if (targetDetail.getStatus() != BookingStatusType.PENDING) {
                continue;
            }

            Room room = targetDetail.getRoom();
            Long hotelId = room.getFloor().getBuilding().getHotel().getId();

            LocalDate checkInDate = targetDetail.getCheckinTime() != null
                    ? targetDetail.getCheckinTime().toLocalDate()
                    : now.toLocalDate();

            BranchRoomPolicy policy = branchRoomPolicyRepository
                    .findByHotelIdAndRoomType(hotelId, room.getRoomType());

            BigDecimal roomPrice = BigDecimal.ZERO;
            if (policy != null) {
                roomPrice = getDailyRoomPrice(room, checkInDate, hotelId, policy);
            }

            targetDetail.setStatus(BookingStatusType.CHECKED_IN);
            targetDetail.setActualCheckInTime(now);

            if (targetDetail.getCheckinTime() != null && now.isBefore(targetDetail.getCheckinTime())) {
                BigDecimal earlyFee = SurchargeCalculator.calculateEarlyCheckInFee(
                        targetDetail.getCheckinTime(),
                        now,
                        roomPrice
                );

                if (earlyFee != null && earlyFee.compareTo(BigDecimal.ZERO) > 0) {
                    targetDetail.setEarlyCheckInFee(earlyFee);
                    batchEarlyFeeTotal = batchEarlyFeeTotal.add(earlyFee);
                }
            }
        }

        if (batchEarlyFeeTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentSurchargeTotal = order.getSurchargeTotalAmount() != null
                    ? order.getSurchargeTotalAmount()
                    : BigDecimal.ZERO;

            order.setSurchargeTotalAmount(currentSurchargeTotal.add(batchEarlyFeeTotal));
            order.setOrderStatus(OrderStatusType.OPEN);
        }

        boolean allCheckedIn = booking.getBookingDetails().stream()
                .allMatch(d ->
                        d.getStatus() == BookingStatusType.CHECKED_IN
                                || d.getStatus() == BookingStatusType.CHECKED_OUT
                                || d.getStatus() == BookingStatusType.CANCELLED
                );

        if (allCheckedIn) {
            booking.setBookingStatus(BookingStatus.IN_HOUSE);
        }

        Booking saved = bookingRepository.save(booking);
        return bookingService.toBookingResponse(saved);
    }

    @Transactional
    @Override
    public BookingResponse processBulkCheckOut(String bookingId, List<Long> bookingDetailIds, String employeeId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        Order order = booking.getOrder();
        BigDecimal batchLateFeeTotal = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        for (Long detailId : bookingDetailIds) {
            BookingDetail targetDetail = booking.getBookingDetails().stream()
                    .filter(d -> d.getId().equals(detailId))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND));

            if (targetDetail.getStatus() != BookingStatusType.CHECKED_IN) {
                continue;
            }

            Room room = targetDetail.getRoom();
            Long hotelId = room.getFloor().getBuilding().getHotel().getId();

            LocalDate checkoutDate = targetDetail.getCheckoutTime() != null
                    ? targetDetail.getCheckoutTime().toLocalDate()
                    : now.toLocalDate();

            BranchRoomPolicy policy = branchRoomPolicyRepository
                    .findByHotelIdAndRoomType(hotelId, room.getRoomType());

            BigDecimal roomPrice = BigDecimal.ZERO;
            if (policy != null) {
                roomPrice = getDailyRoomPrice(room, checkoutDate, hotelId, policy);
            }

            targetDetail.setStatus(BookingStatusType.CHECKED_OUT);
            targetDetail.setActualCheckOutTime(now);

            if (targetDetail.getCheckoutTime() != null && now.isAfter(targetDetail.getCheckoutTime())) {
                BigDecimal lateFee = SurchargeCalculator.calculateLateCheckOutFee(
                        targetDetail.getCheckoutTime(),
                        now,
                        roomPrice
                );

                if (lateFee != null && lateFee.compareTo(BigDecimal.ZERO) > 0) {
                    targetDetail.setLateCheckOutFee(lateFee);
                    batchLateFeeTotal = batchLateFeeTotal.add(lateFee);
                }
            }
        }

        if (batchLateFeeTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentSurchargeTotal = order.getSurchargeTotalAmount() != null
                    ? order.getSurchargeTotalAmount()
                    : BigDecimal.ZERO;

            order.setSurchargeTotalAmount(currentSurchargeTotal.add(batchLateFeeTotal));
            order.setOrderStatus(OrderStatusType.OPEN);
        }

        boolean allCheckedOut = booking.getBookingDetails().stream()
                .allMatch(d ->
                        d.getStatus() == BookingStatusType.CHECKED_OUT
                                || d.getStatus() == BookingStatusType.CANCELLED
                );

        if (allCheckedOut) {
            booking.setBookingStatus(BookingStatus.COMPLETED);
        }

        Booking saved = bookingRepository.save(booking);
        return bookingService.toBookingResponse(saved);
    }
}
