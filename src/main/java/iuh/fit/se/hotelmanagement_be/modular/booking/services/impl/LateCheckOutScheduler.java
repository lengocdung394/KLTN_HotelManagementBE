package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.SurchargeCalculator;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingDetailRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.LateCheckOutBookingNotificationResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.LateRoomDetailResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.enums.SurchargeLevel;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomSeasonalRate;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomSeasonalRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class LateCheckOutScheduler {
    private final BookingDetailRepository bookingDetailRepository;
    private final RoomSeasonalRateRepository roomSeasonalRateRepository;
    private final BranchRoomPolicyRepository branchRoomPolicyRepository;
    private final BookingSocketEmitter bookingSocketEmitter;

    @Scheduled(fixedRate = 60000) // Chạy mỗi 1 phút
    public void checkAndBroadcastLateCheckOuts() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Lấy tất cả các BookingDetail đang CHECKED_IN và có thời gian checkout dự kiến trước thời điểm hiện tại
        List<BookingDetail> overdueDetails = bookingDetailRepository.findByStatusAndCheckoutTimeBefore(
                BookingStatusType.CHECKED_IN, now
        );

        if (overdueDetails == null || overdueDetails.isEmpty()) {
            return;
        }

        // 2. Gom nhóm các BookingDetail này lại theo Booking cha của chúng
        Map<Booking, List<BookingDetail>> groupedByBooking = overdueDetails.stream()
                .collect(Collectors.groupingBy(BookingDetail::getBooking));

        // 3. Duyệt qua từng Booking để tính toán và đóng gói dữ liệu gửi Socket
        for (Map.Entry<Booking, List<BookingDetail>> entry : groupedByBooking.entrySet()) {
            Booking booking = entry.getKey();
            List<BookingDetail> details = entry.getValue();

            if (booking.getHotel() == null) {
                continue;
            }
            Long hotelId = booking.getHotel().getId();

            List<LateRoomDetailResponse> roomDtos = details.stream().map(detail -> {
                LocalDateTime scheduledCheckOut = detail.getCheckoutTime();
                Room room = detail.getRoom();

                // Lấy giá phòng dựa theo chính sách giá phòng theo ngày checkout
                LocalDate checkoutDate = scheduledCheckOut != null ? scheduledCheckOut.toLocalDate() : now.toLocalDate();
                BigDecimal dailyRate = calculateRoomPriceForDate(hotelId, checkoutDate, room);

                // Tính tiền phụ thu và lấy cấp độ phạt tương ứng
                BigDecimal currentSurcharge = SurchargeCalculator.calculateLateCheckOutFee(
                        scheduledCheckOut, now, dailyRate
                );
                SurchargeLevel level = SurchargeCalculator.determineLateCheckOutLevel(
                        scheduledCheckOut, now
                );

                return new LateRoomDetailResponse(
                        detail.getId(),
                        room != null ? room.getRoomNumber() : "N/A",
                        scheduledCheckOut,
                        now,
                        currentSurcharge,
                        level
                );
            }).collect(Collectors.toList());

            // Đóng gói vào DTO tổng của Booking
            LateCheckOutBookingNotificationResponse notification = new LateCheckOutBookingNotificationResponse(
                    booking.getId(),
                    booking.getCustomer() != null ? booking.getCustomer().getFullName() : "N/A",
                    roomDtos
            );

            // 4. Bắn Socket lên cho Frontend theo khách sạn (Hotel ID)
            bookingSocketEmitter.emitLateCheckOutAlert(hotelId, notification);
            log.debug("Emitted late check-out notification for bookingId={}, total late rooms={}",
                    booking.getId(), roomDtos.size());
        }
    }

    /**
     * Hàm phụ trợ lấy giá phòng chuẩn (bao gồm giá theo mùa hoặc giá chính sách + tiện ích)
     */
    public BigDecimal calculateRoomPriceForDate(Long hotelId, LocalDate targetDate, Room room) {
        if (room == null) return BigDecimal.ZERO;

        Optional<RoomSeasonalRate> seasonalRateOpt = roomSeasonalRateRepository.findActiveRateByDate(hotelId, room.getRoomType(), targetDate);
        BranchRoomPolicy policy = branchRoomPolicyRepository.findByHotelIdAndRoomType(hotelId, room.getRoomType());

        double dailyRoomPrice;
        double amenitiesPrice = room.getTotalAmenitiesPrice();

        if (seasonalRateOpt.isPresent()) {
            dailyRoomPrice = seasonalRateOpt.get().getPrice() + amenitiesPrice;
        } else {
            double basePrice = (policy != null && policy.getBasePrice() != null) ? policy.getBasePrice() : 0.0;
            dailyRoomPrice = basePrice + amenitiesPrice;
        }

        return BigDecimal.valueOf(dailyRoomPrice);
    }

}
