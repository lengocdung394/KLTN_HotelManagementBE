package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.impl.EmailService;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.SurchargeCalculator;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.CheckoutRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingDetailRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.CheckInOutRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingDetailForCheckInOutResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.CheckInOutSummaryResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingServiceResponseForHotel;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingService;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.CheckInOutService;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.CheckoutRefund;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.CheckoutRefundStatus;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.PaymentTransaction;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentStatus;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentType;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.CheckoutRefundRepository;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Comparator;
import java.util.UUID;

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
    BookingDetailRepository bookingDetailRepository;
    EmailService emailService;
    CheckoutRefundRepository checkoutRefundRepository;

    @Override
    public CheckInOutSummaryResponse getSummary(Long hotelId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.plusDays(1).atStartOfDay();
        return new CheckInOutSummaryResponse(
                checkInOutRepository.countWaitingCheckIns(hotelId, targetDate),
                checkInOutRepository.countInHouseOnDate(hotelId, dayStart, dayEnd),
                checkInOutRepository.countWaitingCheckOuts(hotelId, targetDate),
                checkInOutRepository.countCompletedCheckOuts(hotelId, targetDate)
        );
    }

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

        LocalDateTime now = LocalDateTime.now();
        return checkInDetails.stream().map(detail -> {
            BookingDetailForCheckInOutResponse response =
                    mapToBookingDetailResponseList(List.of(detail)).get(0);
            BigDecimal estimatedEarlyCheckInFee = BigDecimal.ZERO;

            if (detail.getCheckinTime() != null && now.isBefore(detail.getCheckinTime())) {
                Room room = detail.getRoom();
                Long detailHotelId = room.getFloor().getBuilding().getHotel().getId();
                BranchRoomPolicy policy = branchRoomPolicyRepository
                        .findByHotelIdAndRoomType(detailHotelId, room.getRoomType());
                estimatedEarlyCheckInFee = SurchargeCalculator.calculateEarlyCheckInFee(
                        detail.getCheckinTime(),
                        now,
                        rateDate -> getDailyRoomPrice(room, rateDate, detailHotelId, policy)
                );
            }

            response.setEstimatedEarlyCheckInFee(estimatedEarlyCheckInFee.doubleValue());
            return response;
        }).toList();
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
    public List<BookingDetailForCheckInOutResponse> getEarlyCheckInsByDate(Long hotelId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return mapToBookingDetailResponseList(
                checkInOutRepository.findEarlyCheckInsByHotelAndDate(hotelId, targetDate)
        );
    }

    @Override
    public List<BookingDetailForCheckInOutResponse> mapToBookingDetailResponseList(List<BookingDetail> details) {
        return details.stream().map(detail -> {
            Order order = (detail.getBooking() != null) ? detail.getBooking().getOrder() : null;
            String paymentStatus = (order != null && order.getOrderStatus() != null) ? order.getOrderStatus().name() : "OPEN";
            boolean isPaid = order != null && order.getOrderStatus() == OrderStatusType.CLOSED;

            return BookingDetailForCheckInOutResponse.builder()
                    .roomNumber(detail.getRoom() != null ? detail.getRoom().getRoomNumber() : "-")
                    // Thong tin khach hang
                    .cccd(detail.getBooking() != null && detail.getBooking().getCustomer() != null ? detail.getBooking().getCustomer().getCccd() : null)
                    .nameCustomer(detail.getBooking() != null && detail.getBooking().getCustomer() != null ? detail.getBooking().getCustomer().getFullName() : "Chưa cập nhật")
                    .phone(detail.getBooking() != null && detail.getBooking().getCustomer() != null ? detail.getBooking().getCustomer().getPhone() : null)
                    .bookingId(detail.getBooking() != null ? detail.getBooking().getId() : null)
                    .bookingDetailId(detail.getId())
                    .roomId(detail.getRoom() != null ? detail.getRoom().getId() : null)
                    .roomName(detail.getRoom() != null && detail.getRoom().getRoomType() != null ? detail.getRoom().getRoomType().toString() : null)
                    .roomTypeName(detail.getRoom() != null && detail.getRoom().getRoomType() != null ? detail.getRoom().getRoomType().name() : null)
                    .checkInTime(detail.getCheckinTime())
                    .checkOutTime(detail.getCheckoutTime())
                    .actualCheckInTime(detail.getActualCheckInTime())
                    .numAdults(detail.getNumAdults())
                    .numChildren(detail.getNumChildren())
                    .baseRoomPricePerNight(detail.getBaseRoomPricePerNight())
                    .extraAdultFeePerNight(detail.getExtraAdultFeePerNight())
                    .extraChildFeePerNight(detail.getExtraChildFeePerNight())
                    .paidAmount(order != null && order.getPaidAmount() != null ? order.getPaidAmount().doubleValue() : null)
                    .remainingAmount(order != null ? order.getRemainingAmount().doubleValue() : null)
                    .surplusAmount(order != null && order.getTotalAmount() != null
                            ? (order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount())
                                    .subtract(order.getTotalAmount()).max(BigDecimal.ZERO).doubleValue()
                            : null)
                    .roomSubTotal(detail.getRoomSubTotal())
                    .serviceSubTotal(detail.getServiceSubTotal())
                    .totalPrice(detail.getTotalPrice())
                    .earlyCheckInFee(detail.getEarlyCheckInFee() != null
                            ? detail.getEarlyCheckInFee().doubleValue()
                            : 0.0)
                    .lateCheckOutFee(detail.getLateCheckOutFee() != null
                            ? detail.getLateCheckOutFee().doubleValue()
                            : 0.0)
                    .paymentStatus(paymentStatus)
                    .bookingDetailStatus(detail.getStatus() != null ? detail.getStatus().name() : null)
                    .roomPaid(isPaid)
                    // Moc noi phan dich vu
                    .bookingServiceResponseForHotel(detail.getBookingServiceDetails() != null ?
                            detail.getBookingServiceDetails().stream()
                                    .map(v -> BookingServiceResponseForHotel.builder()
                                            .bookingServiceDetailId(v.getId())
                                            .serviceId(v.getService() != null ? v.getService().getId() : null)
                                            .price(v.getPrice())
                                            .name(v.getName())
                                            .quantity(v.getQuantity())
                                            .cancelled(v.getCancelled())
                                            .cancelledAt(v.getCancelledAt())
                                            .usedAt(v.getUsedAt()).used(v.getUsed()).usedBy(v.getUsedBy()).build()).toList() : Collections.emptyList()
                    ).build();
        }).toList();
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
    public BookingResponse processBulkCheckIn(String bookingId, List<String> bookingDetailIds, String employeeId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        Order order = booking.getOrder();
        LocalDateTime now = LocalDateTime.now();
        Set<String> unavailableRoomNumbers = new LinkedHashSet<>();

        for (String detailId : bookingDetailIds) {
            BookingDetail targetDetail = booking.getBookingDetails().stream()
                    .filter(d -> d.getId().equals(detailId))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND));

            if (targetDetail.getStatus() != BookingStatusType.PENDING) {
                continue;
            }

            Room room = targetDetail.getRoom();

            LocalDateTime currentEffectiveCheckIn = targetDetail.getCheckinTime() != null && now.isBefore(targetDetail.getCheckinTime())
                    ? now
                    : (targetDetail.getCheckinTime() != null ? targetDetail.getCheckinTime() : now);

            boolean isRoomBusy = bookingDetailRepository.existsOverlappingActiveBooking(
                    room.getId(),
                    currentEffectiveCheckIn,
                    targetDetail.getCheckoutTime(),
                    targetDetail.getId()
            );

            if (isRoomBusy) {
                unavailableRoomNumbers.add(room.getRoomNumber());
                continue;
            }

            Long hotelId = room.getFloor().getBuilding().getHotel().getId();
            BranchRoomPolicy policy = branchRoomPolicyRepository
                    .findByHotelIdAndRoomType(hotelId, room.getRoomType());

            targetDetail.setStatus(BookingStatusType.CHECKED_IN);
            targetDetail.setActualCheckInTime(now);

            // Tính tiền phụ thu check-in sớm
            if (targetDetail.getCheckinTime() != null && now.isBefore(targetDetail.getCheckinTime())) {
                BigDecimal earlyFee = SurchargeCalculator.calculateEarlyCheckInFee(
                        targetDetail.getCheckinTime(),
                        now,
                        date -> getDailyRoomPrice(room, date, hotelId, policy)
                );

                if (earlyFee != null && earlyFee.compareTo(BigDecimal.ZERO) > 0) {
                    targetDetail.setEarlyCheckInFee(earlyFee);
                }
            }
        }

        if (!unavailableRoomNumbers.isEmpty()) {
            throw new AppException(
                    ErrorCode.ROOM_ALREADY_OCCUPIED_OR_BOOKED,
                    "Không thể check-in sớm các phòng "
                            + String.join(", ", unavailableRoomNumbers)
                            + " vì đang trùng lịch lưu trú khác. Các phòng này chưa được check-in; hãy kiểm tra lại ma trận phòng."
            );
        }

        // Cập nhật lại toàn bộ tài chính của Order chuẩn xác qua hàm helper chung
        if (order != null) {
            recalculateOrderFinancials(order, booking);
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
    public BookingResponse prepareBulkCheckOut(String bookingId, List<String> bookingDetailIds) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            return bookingService.toBookingResponse(booking);
        }

        LocalDateTime now = LocalDateTime.now();
        for (String detailId : bookingDetailIds == null ? List.<String>of() : bookingDetailIds) {
            BookingDetail targetDetail = booking.getBookingDetails().stream()
                    .filter(detail -> detail.getId().equals(detailId))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND));
            if (targetDetail.getStatus() != BookingStatusType.CHECKED_IN
                    || targetDetail.getCheckoutTime() == null
                    || !now.isAfter(targetDetail.getCheckoutTime())) {
                continue;
            }

            Room room = targetDetail.getRoom();
            Long hotelId = room.getFloor().getBuilding().getHotel().getId();
            BranchRoomPolicy policy = branchRoomPolicyRepository
                    .findByHotelIdAndRoomType(hotelId, room.getRoomType());
            BigDecimal roomPrice = getDailyRoomPrice(
                    room,
                    targetDetail.getCheckoutTime().toLocalDate(),
                    hotelId,
                    policy
            );
            BigDecimal lateFee = SurchargeCalculator.calculateLateCheckOutFee(
                    targetDetail.getCheckoutTime(),
                    now,
                    roomPrice
            );
            targetDetail.setLateCheckOutFee(lateFee);
        }

        Order order = booking.getOrder();
        if (order != null) {
            recalculateOrderFinancials(order, booking);
        }
        Booking saved = bookingRepository.save(booking);
        return bookingService.toBookingResponse(saved);
    }

    @Transactional
    @Override
    public BookingResponse processBulkCheckOut(String bookingId, CheckoutRequest request, String employeeId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        Order order = booking.getOrder();
        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            return bookingService.toBookingResponse(booking);
        }
        List<String> bookingDetailIds = request == null ? List.of() : request.getBookingDetailIds();
        LocalDateTime now = LocalDateTime.now();
        for (String detailId : bookingDetailIds) {
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

            BigDecimal roomPrice = getDailyRoomPrice(room, checkoutDate, hotelId, policy);

            // Tính phí trễ hạn checkout
            if (targetDetail.getCheckoutTime() != null && now.isAfter(targetDetail.getCheckoutTime())) {
                BigDecimal lateFee = SurchargeCalculator.calculateLateCheckOutFee(
                        targetDetail.getCheckoutTime(),
                        now,
                        roomPrice
                );

                if (lateFee != null && lateFee.compareTo(BigDecimal.ZERO) > 0) {
                    targetDetail.setLateCheckOutFee(lateFee);
                }
            }

            targetDetail.setStatus(BookingStatusType.CHECKED_OUT);
            targetDetail.setActualCheckOutTime(now);
        }

        // Cập nhật lại toàn bộ tài chính của Order (bao gồm cả phí trễ hạn nếu có)
        if (order != null) {
            recalculateOrderFinancials(order, booking);
            if (order.getRemainingAmount() != null
                    && order.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
                throw new AppException(ErrorCode.CHECKOUT_PAYMENT_REQUIRED);
            }
        }

        boolean allCheckedOut = booking.getBookingDetails().stream()
                .allMatch(d ->
                        d.getStatus() == BookingStatusType.CHECKED_OUT
                                || d.getStatus() == BookingStatusType.CANCELLED
                );

        // tat ca phong dc checkout xong thi luc nay ma khuyen mai moi duoc tinh la la duoc su dung
        if (allCheckedOut && booking.getBookingStatus() != BookingStatus.COMPLETED) {
            booking.setBookingStatus(BookingStatus.COMPLETED);
        }

        if (allCheckedOut && order != null && checkoutRefundRepository.findByBookingId(bookingId).isEmpty()) {
            BigDecimal paidAmount = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
            BigDecimal refundAmount = paidAmount.subtract(order.getTotalAmount()).max(BigDecimal.ZERO);
            if (refundAmount.signum() > 0) {
                PaymentType refundMethod = request == null ? null : request.getSurplusRefundMethod();
                if (refundMethod == null) {
                    throw new AppException(ErrorCode.CHECKOUT_REFUND_METHOD_REQUIRED);
                }
                if (refundMethod != PaymentType.CASH && refundMethod != PaymentType.BANK) {
                    throw new AppException(ErrorCode.CHECKOUT_REFUND_INVALID_METHOD);
                }
                if (refundMethod == PaymentType.CASH) {
                    CheckoutRefund refund = new CheckoutRefund();
                    refund.setId("CR_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
                    refund.setBooking(booking);
                    refund.setAmount(refundAmount);
                    refund.setRefundMethod(PaymentType.CASH);
                    refund.setStatus(CheckoutRefundStatus.PENDING);
                    refund.setNote(request.getRefundNote() == null || request.getRefundNote().isBlank()
                            ? "Chờ nhân viên xác nhận hoàn tiền mặt tại quầy"
                            : request.getRefundNote().trim());
                    checkoutRefundRepository.save(refund);
                } else {
                    RefundBankDetails bankDetails = resolveCheckoutRefundBankDetails(order);
                    CheckoutRefund refund = new CheckoutRefund();
                    refund.setId("CR_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
                    refund.setBooking(booking);
                    refund.setAmount(refundAmount);
                    refund.setRefundMethod(PaymentType.BANK);
                    refund.setStatus(CheckoutRefundStatus.PENDING);
                    refund.setBankName(bankDetails.bankName());
                    refund.setBankCode(bankDetails.bankCode());
                    refund.setBankAccountName(bankDetails.bankAccountName());
                    refund.setBankAccountNumber(bankDetails.bankAccountNumber());
                    refund.setNote(request.getRefundNote());
                    checkoutRefundRepository.save(refund);
                }
            }
        }

        Booking saved = bookingRepository.save(booking);
        if (allCheckedOut) {
            sendCheckoutInvoiceAfterCommit(saved, order, now);
        }
        return bookingService.toBookingResponse(saved);
    }

    private RefundBankDetails resolveCheckoutRefundBankDetails(Order order) {
        if (order.getPaymentTransactions() == null) {
            return RefundBankDetails.empty();
        }
        return order.getPaymentTransactions().stream()
                .filter(transaction -> transaction.getCashFlowType() == CashFlowType.RECEIPT)
                .filter(transaction -> transaction.getCounterAccountName() != null
                        && !transaction.getCounterAccountName().isBlank()
                        && transaction.getCounterAccountNumber() != null
                        && !transaction.getCounterAccountNumber().isBlank()
                        && ((transaction.getCounterAccountBankName() != null
                        && !transaction.getCounterAccountBankName().isBlank())
                        || (transaction.getCounterAccountBankId() != null
                        && !transaction.getCounterAccountBankId().isBlank())))
                .max(Comparator.comparing(
                        PaymentTransaction::getTransactionDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(transaction -> new RefundBankDetails(
                        blankToNull(transaction.getCounterAccountBankName())
                                == null ? blankToNull(transaction.getCounterAccountBankId())
                                : blankToNull(transaction.getCounterAccountBankName()),
                        blankToNull(transaction.getCounterAccountBankId()),
                        blankToNull(transaction.getCounterAccountName()),
                        blankToNull(transaction.getCounterAccountNumber())))
                .orElseGet(RefundBankDetails::empty);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record RefundBankDetails(String bankName, String bankCode, String bankAccountName, String bankAccountNumber) {
        private static RefundBankDetails empty() {
            return new RefundBankDetails(null, null, null, null);
        }
    }

    private void sendCheckoutInvoiceAfterCommit(Booking booking, Order order, LocalDateTime checkoutTime) {
        if (booking.getCustomer() == null) {
            log.warn("Bỏ qua hóa đơn email cho booking {} vì không tìm thấy khách hàng", booking.getId());
            return;
        }
        String email = emailService.getCustomerEmail(booking.getCustomer());
        if (email == null) {
            log.info("Bỏ qua hóa đơn email cho booking {} vì khách hàng chưa có email", booking.getId());
            return;
        }

        String customerName = booking.getCustomer().getFullName();
        String bookingId = booking.getId();
        String hotelName = booking.getHotel() != null ? booking.getHotel().getName() : "Khách sạn";
        String invoiceDetails = buildInvoiceDetails(booking, order, checkoutTime);

        emailService.sendAfterCommit("checkout invoice for booking " + bookingId, () ->
                emailService.sendCheckoutInvoiceEmail(
                        email,
                        customerName,
                        bookingId,
                        hotelName,
                        invoiceDetails
                ));
    }

    private String buildInvoiceDetails(Booking booking, Order order, LocalDateTime checkoutTime) {
        StringBuilder invoice = new StringBuilder()
                .append("Ngày checkout: ").append(checkoutTime).append("\n")
                .append("Các phòng:\n");
        booking.getBookingDetails().forEach(detail -> invoice
                .append("- Phòng ")
                .append(detail.getRoom() != null ? detail.getRoom().getId() : "N/A")
                .append(" | Tiền phòng: ").append(detail.getRoomSubTotal() != null ? detail.getRoomSubTotal() : 0)
                .append(" VND | Dịch vụ: ").append(detail.getServiceSubTotal() != null ? detail.getServiceSubTotal() : 0)
                .append(" VND | Phụ thu: ")
                .append((detail.getEarlyCheckInFee() != null ? detail.getEarlyCheckInFee() : BigDecimal.ZERO)
                        .add(detail.getLateCheckOutFee() != null ? detail.getLateCheckOutFee() : BigDecimal.ZERO)
                        .add(detail.getOtherSurcharges() != null ? detail.getOtherSurcharges() : BigDecimal.ZERO))
                .append(" VND\n"));

        if (order == null) {
            return invoice.toString();
        }
        return invoice
                .append("\nTiền phòng: ").append(formatInvoiceAmount(order.getRoomTotalAmount())).append("\n")
                .append("Tiền dịch vụ: ").append(formatInvoiceAmount(order.getServiceTotalAmount())).append("\n")
                .append("Phụ thu: ").append(formatInvoiceAmount(order.getSurchargeTotalAmount())).append("\n")
                .append("Giảm giá: ").append(formatInvoiceAmount(order.getDiscountAmountTotal())).append("\n")
                .append("Tổng hóa đơn: ").append(formatInvoiceAmount(order.getTotalAmount())).append("\n")
                .append("Đã thanh toán: ").append(formatInvoiceAmount(order.getPaidAmount())).append("\n")
                .append("Còn phải thanh toán: ").append(formatInvoiceAmount(order.getRemainingAmount()))
                .toString();
    }

    private BigDecimal formatInvoiceAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private void recalculateOrderFinancials(Order order, Booking booking) {
        // 1. Tính tổng phụ thu từ tất cả các booking detail
        BigDecimal totalSurcharge = booking.getBookingDetails().stream()
                .filter(detail -> detail.getStatus() != BookingStatusType.CANCELLED)
                .map(d -> {
                    BigDecimal early = d.getEarlyCheckInFee() != null ? d.getEarlyCheckInFee() : BigDecimal.ZERO;
                    BigDecimal late = d.getLateCheckOutFee() != null ? d.getLateCheckOutFee() : BigDecimal.ZERO;
                    BigDecimal other = d.getOtherSurcharges() != null ? d.getOtherSurcharges() : BigDecimal.ZERO;
                    return early.add(late).add(other);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal roomTotal = order.getRoomTotalAmount() != null ? order.getRoomTotalAmount() : BigDecimal.ZERO;
        BigDecimal serviceTotal = order.getServiceTotalAmount() != null ? order.getServiceTotalAmount() : BigDecimal.ZERO;
        BigDecimal discountRoom = order.getDiscountRoomAmount() != null ? order.getDiscountRoomAmount() : BigDecimal.ZERO;
        BigDecimal discountService = order.getDiscountServiceAmount() != null ? order.getDiscountServiceAmount() : BigDecimal.ZERO;
        BigDecimal discountTotal = order.getDiscountAmountTotal() != null ? order.getDiscountAmountTotal() : BigDecimal.ZERO;
        BigDecimal paidAmount = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;

        // 2. Tính lại tổng tiền: (Tiền phòng + Tiền dịch vụ + Tổng phụ thu) - Giảm giá
        BigDecimal subTotal = roomTotal.add(serviceTotal).add(totalSurcharge);
        BigDecimal newTotalAmount = subTotal
                .subtract(discountRoom)
                .subtract(discountService)
                .subtract(discountTotal)
                .max(BigDecimal.ZERO);

        // 3. Tính lại số tiền còn lại phải trả: Tổng tiền - Đã thanh toán
        BigDecimal remainingAmount = newTotalAmount.subtract(paidAmount).max(BigDecimal.ZERO);

        // 4. Gán ngược lại vào Order
        order.setSurchargeTotalAmount(totalSurcharge);
        order.setTotalAmount(newTotalAmount);
        order.setRemainingAmount(remainingAmount);
        order.setOrderStatus(OrderStatusType.OPEN);
        order.setPaymentStatus(remainingAmount.signum() == 0
                ? PaymentStatus.PAID
                : paidAmount.signum() > 0
                ? PaymentStatus.PARTIAL
                : PaymentStatus.UNPAID);
    }
}
