package iuh.fit.se.hotelmanagement_be.modular.closure.services;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingNightlyPrice;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingServiceDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.CheckoutRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.CheckInOutService;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.closure.entities.HotelClosureResolution;
import iuh.fit.se.hotelmanagement_be.modular.closure.entities.HotelClosureSchedule;
import iuh.fit.se.hotelmanagement_be.modular.closure.repositories.HotelClosureResolutionRepository;
import iuh.fit.se.hotelmanagement_be.modular.closure.repositories.HotelClosureScheduleRepository;
import iuh.fit.se.hotelmanagement_be.modular.closure.requests.CreateHotelClosureRequest;
import iuh.fit.se.hotelmanagement_be.modular.closure.requests.ResolveHotelClosureRequest;
import iuh.fit.se.hotelmanagement_be.modular.closure.responses.HotelClosureAffectedBookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.closure.responses.HotelClosureResponse;
import iuh.fit.se.hotelmanagement_be.modular.closure.responses.PublicHotelClosureResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.services.impl.BookingCancellationService;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import iuh.fit.se.hotelmanagement_be.modular.notification.services.NotificationService;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HotelClosureService {
    HotelClosureScheduleRepository scheduleRepository;
    HotelClosureResolutionRepository resolutionRepository;
    HotelRepository hotelRepository;
    BookingRepository bookingRepository;
    NotificationService notificationService;
    BookingCancellationService cancellationService;
    CheckInOutService checkInOutService;

    @Transactional
    public HotelClosureResponse createSchedule(CreateHotelClosureRequest request, Account creator) {
        requireRole(creator, Set.of("ROLE_SUPER_ADMIN"));
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày kết thúc phải từ ngày bắt đầu trở đi.");
        }
        List<Long> hotelIds = request.getHotelIds() == null
                ? List.of()
                : request.getHotelIds().stream().filter(Objects::nonNull).distinct().toList();
        if (!request.isAllHotels() && hotelIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hãy chọn ít nhất một khách sạn.");
        }
        if (request.isAllHotels()) {
            hotelIds = hotelRepository.findAll().stream().map(Hotel::getId).toList();
        } else if (hotelIds.stream().anyMatch(id -> !hotelRepository.existsById(id))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách có khách sạn không tồn tại.");
        }
        List<Long> selectedHotelIds = hotelIds;
        List<HotelClosureSchedule> conflictingSchedules = scheduleRepository.findAllByActiveTrue().stream()
                .filter(existing -> scheduleConflictsWith(
                        existing, request.getStartDate(), request.getEndDate(),
                        request.isAllHotels(), selectedHotelIds))
                .toList();
        if (!conflictingSchedules.isEmpty()) {
            HotelClosureSchedule conflict = conflictingSchedules.get(0);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    String.format("Lịch bị trùng ngày với \"%s\" (%s đến %s) tại ít nhất một chi nhánh áp dụng.",
                            conflict.getTitle(), conflict.getStartDate(), conflict.getEndDate()));
        }

        HotelClosureSchedule schedule = new HotelClosureSchedule();
        schedule.setTitle(request.getTitle().trim());
        schedule.setDescription(request.getDescription());
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());
        schedule.setAllHotels(request.isAllHotels());
        schedule.setHotelIds(request.isAllHotels() ? List.of() : new ArrayList<>(hotelIds));
        schedule.setCreatedBy(creator);
        HotelClosureSchedule saved = scheduleRepository.save(schedule);

        String description = String.format("%s: %s đến %s.%s",
                saved.getTitle(),
                saved.getStartDate(),
                saved.getEndDate(),
                saved.getDescription() == null || saved.getDescription().isBlank()
                        ? ""
                        : " " + saved.getDescription().trim());
        if (saved.isAllHotels()) {
            notificationService.createAndPublishSystemWide(
                    NotificationType.SYSTEM, "Lịch tạm đóng khách sạn", description,
                    "HOTEL_CLOSURE", saved.getId(), hotelIds);
        } else {
            for (Long hotelId : hotelIds) {
                notificationService.createAndPublishInCurrentTransaction(
                        hotelId, NotificationType.SYSTEM, "Lịch tạm đóng khách sạn", description,
                        "HOTEL_CLOSURE", saved.getId(), true, false);
            }
        }
        return toResponse(saved);
    }

    boolean scheduleConflictsWith(
            HotelClosureSchedule existing,
            LocalDate startDate,
            LocalDate endDate,
            boolean allHotels,
            List<Long> hotelIds) {
        boolean datesOverlap = !existing.getStartDate().isAfter(endDate)
                && !existing.getEndDate().isBefore(startDate);
        if (!datesOverlap) {
            return false;
        }
        if (allHotels || existing.isAllHotels()) {
            return true;
        }
        return existing.getHotelIds() != null
                && hotelIds.stream().anyMatch(existing.getHotelIds()::contains);
    }

    @Transactional(readOnly = true)
    public List<HotelClosureResponse> listSchedules(Account account) {
        requireRole(account, Set.of(
                "ROLE_SUPER_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE", "ROLE_RECEPTIONIST", "ROLE_STAFF"));
        List<HotelClosureSchedule> schedules = account.getHotelId() == null
                ? scheduleRepository.findAll().stream()
                    .sorted((left, right) -> right.getStartDate().compareTo(left.getStartDate())).toList()
                : scheduleRepository.findActiveForHotel(account.getHotelId());
        return schedules.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PublicHotelClosureResponse> listPublicSchedules(Long hotelId) {
        if (hotelId == null || !hotelRepository.existsById(hotelId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách sạn.");
        }
        return scheduleRepository.findUpcomingForHotel(hotelId, LocalDate.now()).stream()
                .map(schedule -> new PublicHotelClosureResponse(
                        schedule.getId(), schedule.getStartDate(), schedule.getEndDate()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HotelClosureAffectedBookingResponse> getAffectedBookings(String scheduleId, Account account) {
        requireStaffRole(account);
        HotelClosureSchedule schedule = getAccessibleSchedule(scheduleId, account);
        Long hotelId = account.getHotelId();
        List<Booking> bookings = bookingRepository.findAllByHotelId(hotelId);
        return bookings.stream()
                .filter(booking -> booking.getBookingDetails() != null
                        && booking.getBookingDetails().stream().anyMatch(detail -> overlaps(schedule, detail)))
                .filter(booking -> booking.getBookingStatus() != null && !booking.getBookingStatus().isCancelled()
                        && booking.getBookingStatus() != BookingStatus.COMPLETED)
                .map(booking -> {
                    List<HotelClosureAffectedBookingResponse.RoomStay> stays = booking.getBookingDetails().stream()
                            .filter(detail -> overlaps(schedule, detail))
                            .map(detail -> new HotelClosureAffectedBookingResponse.RoomStay(
                                    detail.getId(),
                                    detail.getRoom() == null ? null : detail.getRoom().getRoomNumber(),
                                    detail.getCheckinTime() == null ? null : detail.getCheckinTime().toLocalDate(),
                                    detail.getCheckoutTime() == null ? null : detail.getCheckoutTime().toLocalDate(),
                                            detail.getStatus() == null ? null : detail.getStatus().name(),
                                            affectedServiceStays(detail)))
                            .toList();
                    String guestName = booking.getCustomer() == null ? "Khách hàng"
                            : booking.getCustomer().getFullName();
                    String action = resolutionRepository.findBySchedule_IdAndBooking_Id(scheduleId, booking.getId())
                            .map(HotelClosureResolution::getAction).orElse(null);
                    var cancellationPreview = cancellationService.previewHotelClosureCancellation(booking);
                    List<BookingDetail> affectedInHouseDetails = booking.getBookingDetails().stream()
                            .filter(detail -> detail.getStatus() == BookingStatusType.CHECKED_IN
                                    && overlaps(schedule, detail))
                            .toList();
                    var earlyCheckoutPreview = previewEarlyCheckout(booking, affectedInHouseDetails);
                    return new HotelClosureAffectedBookingResponse(
                            booking.getId(),
                            guestName,
                            booking.getBookingStatus().name(),
                            stays,
                            action,
                            cancellationPreview.getRefundAmount(),
                            cancellationPreview.getRoomRefundAmount(),
                            cancellationPreview.getServiceRefundAmount(),
                            cancellationPreview.getRoomPenaltyAmount(),
                            cancellationPreview.getRefundPolicy(),
                            earlyCheckoutPreview.refundAmount(),
                            earlyCheckoutPreview.originalRoomAmount(),
                            earlyCheckoutPreview.originalRoomNights(),
                            earlyCheckoutPreview.usedRoomNights(),
                            earlyCheckoutPreview.roomAmount(),
                            earlyCheckoutPreview.earlyCheckInFeeAmount(),
                            earlyCheckoutPreview.unpaidBeforeCheckoutAmount(),
                            earlyCheckoutPreview.outstandingAmount(),
                            earlyCheckoutPreview.bookingTotalAmount(),
                            earlyCheckoutPreview.paidAmount(),
                            earlyCheckoutPreview.projectedTotalAmount());
                })
                .toList();
    }

    BigDecimal previewEarlyCheckoutRefund(Booking booking, List<BookingDetail> affectedDetails) {
        return previewEarlyCheckout(booking, affectedDetails).refundAmount();
    }

    EarlyCheckoutPreview previewEarlyCheckout(Booking booking, List<BookingDetail> affectedDetails) {
        Order order = booking.getOrder();
        if (order == null || affectedDetails.isEmpty()) {
            return new EarlyCheckoutPreview(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    0,
                    0,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        LocalDateTime checkoutAt = LocalDateTime.now();
        LocalDate checkoutDate = checkoutAt.toLocalDate();
        BigDecimal originalRoomTotal = BigDecimal.ZERO;
        BigDecimal projectedRoomTotal = BigDecimal.ZERO;
        BigDecimal projectedSurchargeTotal = BigDecimal.ZERO;
        BigDecimal earlyCheckInFeeAmount = BigDecimal.ZERO;
        BigDecimal originalAffectedRoomAmount = BigDecimal.ZERO;
        BigDecimal projectedAffectedRoomAmount = BigDecimal.ZERO;
        int originalAffectedRoomNights = 0;
        int usedAffectedRoomNights = 0;
        for (BookingDetail detail : booking.getBookingDetails()) {
            BigDecimal roomTotal = BigDecimal.valueOf(
                    detail.getRoomSubTotal() == null ? 0 : detail.getRoomSubTotal());
            if (detail.getStatus() != BookingStatusType.CANCELLED) {
                originalRoomTotal = originalRoomTotal.add(roomTotal);
                earlyCheckInFeeAmount = earlyCheckInFeeAmount.add(
                        detail.getEarlyCheckInFee() == null ? BigDecimal.ZERO : detail.getEarlyCheckInFee());
                if (affectedDetails.contains(detail)) {
                    originalAffectedRoomAmount = originalAffectedRoomAmount.add(roomTotal);
                    originalAffectedRoomNights += roomNights(detail);
                    usedAffectedRoomNights += usedRoomNights(detail, checkoutDate);
                    roomTotal = detail.getNightlyPrices() == null
                            ? prorateRoomAmount(detail, checkoutDate)
                            : usedRoomAmount(detail, checkoutDate);
                    projectedAffectedRoomAmount = projectedAffectedRoomAmount.add(roomTotal);
                    BigDecimal lateFee = checkInOutService.previewLateCheckOutFee(detail, checkoutAt);
                    projectedSurchargeTotal = projectedSurchargeTotal.add(lateFee);
                }
                projectedRoomTotal = projectedRoomTotal.add(roomTotal);
                projectedSurchargeTotal = projectedSurchargeTotal
                        .add(detail.getEarlyCheckInFee() == null ? BigDecimal.ZERO : detail.getEarlyCheckInFee())
                        .add(detail.getOtherSurcharges() == null ? BigDecimal.ZERO : detail.getOtherSurcharges());
                if (!affectedDetails.contains(detail)) {
                    projectedSurchargeTotal = projectedSurchargeTotal
                            .add(detail.getLateCheckOutFee() == null ? BigDecimal.ZERO : detail.getLateCheckOutFee());
                }
            } else {
                projectedSurchargeTotal = projectedSurchargeTotal
                        .add(detail.getOtherSurcharges() == null ? BigDecimal.ZERO : detail.getOtherSurcharges());
            }
        }

        BigDecimal serviceTotal = BigDecimal.ZERO;
        for (BookingDetail detail : booking.getBookingDetails()) {
            if (detail.getStatus() == BookingStatusType.CANCELLED
                    || detail.getBookingServiceDetails() == null) {
                continue;
            }
            for (BookingServiceDetail service : detail.getBookingServiceDetails()) {
                if (Boolean.TRUE.equals(service.getCancelled())) {
                    continue;
                }
                int retainedQuantity = affectedDetails.contains(detail)
                        ? retainedServiceQuantity(service)
                        : Math.max(0, service.getQuantity());
                serviceTotal = serviceTotal.add(BigDecimal.valueOf(
                                service.getPrice() == null ? 0 : service.getPrice())
                        .multiply(BigDecimal.valueOf(retainedQuantity)));
            }
        }

        BigDecimal discountRoom = order.getDiscountRoomAmount() == null
                ? BigDecimal.ZERO
                : order.getDiscountRoomAmount();
        BigDecimal discountService = order.getDiscountServiceAmount() == null
                ? BigDecimal.ZERO
                : order.getDiscountServiceAmount();
        BigDecimal discountTotal = order.getDiscountAmountTotal() == null
                ? BigDecimal.ZERO
                : order.getDiscountAmountTotal();
        if (originalRoomTotal.signum() > 0) {
            BigDecimal retainedRoomRatio = projectedRoomTotal.min(originalRoomTotal)
                    .divide(originalRoomTotal, 8, RoundingMode.HALF_UP);
            discountRoom = discountRoom.multiply(retainedRoomRatio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal originalServiceTotal = order.getServiceTotalAmount() == null
                    ? BigDecimal.ZERO
                    : order.getServiceTotalAmount();
            if (originalServiceTotal.signum() > 0) {
                discountService = discountService.multiply(serviceTotal.min(originalServiceTotal))
                        .divide(originalServiceTotal, 2, RoundingMode.HALF_UP);
            }
            BigDecimal originalSubtotal = originalRoomTotal
                    .add(originalServiceTotal)
                    .add(order.getSurchargeTotalAmount() == null ? BigDecimal.ZERO : order.getSurchargeTotalAmount());
            BigDecimal retainedSubtotal = projectedRoomTotal
                    .add(serviceTotal)
                    .add(order.getSurchargeTotalAmount() == null ? BigDecimal.ZERO : order.getSurchargeTotalAmount());
            if (originalSubtotal.signum() > 0) {
                discountTotal = discountTotal.multiply(retainedSubtotal)
                        .divide(originalSubtotal, 2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal projectedTotal = projectedRoomTotal
                .add(serviceTotal)
                .add(projectedSurchargeTotal)
                .subtract(discountRoom)
                .subtract(discountService)
                .subtract(discountTotal)
                .max(BigDecimal.ZERO);
        BigDecimal paidAmount = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        BigDecimal bookingTotalAmount = order.getTotalAmount().max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal unpaidBeforeCheckoutAmount = order.getTotalAmount().subtract(paidAmount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal refundAmount = paidAmount.subtract(projectedTotal).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal outstandingAmount = projectedTotal.subtract(paidAmount).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        return new EarlyCheckoutPreview(
                refundAmount,
                originalAffectedRoomAmount.setScale(2, RoundingMode.HALF_UP),
                originalAffectedRoomNights,
                usedAffectedRoomNights,
                projectedAffectedRoomAmount.setScale(2, RoundingMode.HALF_UP),
                earlyCheckInFeeAmount.setScale(2, RoundingMode.HALF_UP),
                unpaidBeforeCheckoutAmount,
                outstandingAmount,
                bookingTotalAmount,
                paidAmount.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                projectedTotal.setScale(2, RoundingMode.HALF_UP));
    }

    private int roomNights(BookingDetail detail) {
        if (detail.getNightlyPrices() != null && !detail.getNightlyPrices().isEmpty()) {
            return detail.getNightlyPrices().size();
        }
        if (detail.getCheckinTime() == null || detail.getCheckoutTime() == null) {
            return 0;
        }
        return (int) Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(
                detail.getCheckinTime().toLocalDate(), detail.getCheckoutTime().toLocalDate()));
    }

    private int usedRoomNights(BookingDetail detail, LocalDate checkoutDate) {
        int nightsBeforeCheckout = detail.getNightlyPrices() == null
                ? (int) Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(
                        detail.getCheckinTime().toLocalDate(), checkoutDate))
                : (int) detail.getNightlyPrices().stream()
                        .filter(price -> price.getStayDate() != null && price.getStayDate().isBefore(checkoutDate))
                        .count();
        int totalNights = roomNights(detail);
        if (totalNights > 0 && detail.getCheckinTime() != null
                && !checkoutDate.isAfter(detail.getCheckinTime().toLocalDate())
                && nightsBeforeCheckout == 0) {
            return 1;
        }
        return Math.min(totalNights, nightsBeforeCheckout);
    }

    record EarlyCheckoutPreview(
            BigDecimal refundAmount,
            BigDecimal originalRoomAmount,
            int originalRoomNights,
            int usedRoomNights,
            BigDecimal roomAmount,
            BigDecimal earlyCheckInFeeAmount,
            BigDecimal unpaidBeforeCheckoutAmount,
            BigDecimal outstandingAmount,
            BigDecimal bookingTotalAmount,
            BigDecimal paidAmount,
            BigDecimal projectedTotalAmount) {
    }

    private List<HotelClosureAffectedBookingResponse.ServiceStay> affectedServiceStays(BookingDetail detail) {
        if (detail.getBookingServiceDetails() == null) {
            return List.of();
        }
        return detail.getBookingServiceDetails().stream()
                .filter(service -> !Boolean.TRUE.equals(service.getCancelled()))
                .map(service -> {
                    int quantity = Math.max(0, service.getQuantity());
                    int usedQuantity = Math.min(quantity, Math.max(0, service.getUsedQuantity()));
                    int refundableQuantity = refundableServiceQuantity(service);
                    BigDecimal refundableAmount = BigDecimal.valueOf(
                                    service.getPrice() == null ? 0 : service.getPrice())
                            .multiply(BigDecimal.valueOf(refundableQuantity))
                            .setScale(2, RoundingMode.HALF_UP);
                    return new HotelClosureAffectedBookingResponse.ServiceStay(
                            service.getName(),
                            quantity,
                            usedQuantity,
                            refundableQuantity,
                            refundableAmount);
                })
                .toList();
    }

    private int retainedServiceQuantity(BookingServiceDetail service) {
        int quantity = Math.max(0, service.getQuantity());
        int usedQuantity = Math.min(quantity, Math.max(0, service.getUsedQuantity()));
        return Boolean.TRUE.equals(service.getUsed()) && usedQuantity == 0 ? quantity : usedQuantity;
    }

    private int refundableServiceQuantity(BookingServiceDetail service) {
        int quantity = Math.max(0, service.getQuantity());
        int usedQuantity = Math.min(quantity, Math.max(0, service.getUsedQuantity()));
        if (Boolean.TRUE.equals(service.getUsed()) && usedQuantity == 0) {
            return 0;
        }
        return quantity - usedQuantity;
    }

    @Transactional
    public void resolveBooking(
            String scheduleId,
            String bookingId,
            ResolveHotelClosureRequest request,
            Account account) {
        requireStaffRole(account);
        HotelClosureSchedule schedule = getAccessibleSchedule(scheduleId, account);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if (booking.getHotel() == null || !booking.getHotel().getId().equals(account.getHotelId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Booking không thuộc chi nhánh của bạn.");
        }
        boolean overlaps = booking.getBookingDetails() != null
                && booking.getBookingDetails().stream().anyMatch(detail -> overlaps(schedule, detail));
        if (!overlaps) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking không bị ảnh hưởng bởi lịch này.");
        }
        if (resolutionRepository.findBySchedule_IdAndBooking_Id(scheduleId, bookingId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking này đã được xử lý cho lịch đóng cửa.");
        }

        String action = request.getAction().trim().toUpperCase();
        if ("CANCEL_FULL_REFUND".equals(action)) {
            cancellationService.requestHotelClosureCancellation(
                    bookingId, request.getCancellation(), account);
        } else if ("EARLY_CHECKOUT".equals(action)) {
            if (request.getCheckout() == null || request.getCheckout().getBookingDetailIds() == null
                    || request.getCheckout().getBookingDetailIds().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hãy chọn phòng cần checkout sớm.");
            }
            if (booking.getBookingStatus() != BookingStatus.IN_HOUSE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Chỉ booking đã check-in mới được xử lý theo hướng checkout sớm.");
            }
            LocalDate checkoutDate = LocalDate.now();
            BigDecimal originalRoomTotal = booking.getBookingDetails().stream()
                    .filter(detail -> detail.getStatus() != BookingStatusType.CANCELLED)
                    .map(detail -> BigDecimal.valueOf(detail.getRoomSubTotal() == null ? 0 : detail.getRoomSubTotal()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            List<BookingDetail> affectedInHouseDetails = booking.getBookingDetails().stream()
                    .filter(detail -> detail.getStatus() == BookingStatusType.CHECKED_IN
                            && overlaps(schedule, detail))
                    .toList();
            if (affectedInHouseDetails.isEmpty()
                    || !request.getCheckout().getBookingDetailIds().containsAll(
                            affectedInHouseDetails.stream().map(BookingDetail::getId).toList())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Phải checkout tất cả phòng đang lưu trú bị ảnh hưởng bởi lịch đóng cửa.");
            }
            checkInOutService.cancelUnusedServicesBeforeCheckout(
                    bookingId,
                    affectedInHouseDetails.stream().map(BookingDetail::getId).toList(),
                    account.getEmployeeId());
            for (String detailId : request.getCheckout().getBookingDetailIds()) {
                BookingDetail detail = booking.getBookingDetails().stream()
                        .filter(item -> item.getId().equals(detailId))
                        .findFirst()
                        .orElseThrow(() -> new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND));
                if (detail.getStatus() != BookingStatusType.CHECKED_IN || !overlaps(schedule, detail)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Chỉ phòng đang lưu trú và bị ảnh hưởng bởi lịch mới được checkout sớm.");
                }
                BigDecimal usedRoomAmount = detail.getNightlyPrices() == null
                        ? prorateRoomAmount(detail, checkoutDate)
                        : usedRoomAmount(detail, checkoutDate);
                detail.setRoomSubTotal(usedRoomAmount.doubleValue());
            }
            BigDecimal usedRoomTotal = booking.getBookingDetails().stream()
                    .filter(detail -> detail.getStatus() != BookingStatusType.CANCELLED)
                    .map(detail -> BigDecimal.valueOf(detail.getRoomSubTotal() == null ? 0 : detail.getRoomSubTotal()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            prorateOrderDiscounts(booking.getOrder(), originalRoomTotal, usedRoomTotal);
            checkInOutService.processBulkCheckOut(bookingId, request.getCheckout(), account.getEmployeeId());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hướng xử lý không hợp lệ.");
        }

        HotelClosureResolution resolution = new HotelClosureResolution();
        resolution.setSchedule(schedule);
        resolution.setBooking(booking);
        resolution.setAction(action);
        resolution.setResolvedBy(account);
        resolutionRepository.save(resolution);
    }

    private HotelClosureSchedule getAccessibleSchedule(String scheduleId, Account account) {
        HotelClosureSchedule schedule = scheduleRepository.findByIdAndActiveTrue(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch đóng cửa."));
        if (account.getHotelId() != null
                && !schedule.isAllHotels()
                && !schedule.getHotelIds().contains(account.getHotelId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lịch này không áp dụng cho chi nhánh của bạn.");
        }
        return schedule;
    }

    private void requireStaffRole(Account account) {
        requireRole(account, Set.of("ROLE_MANAGER", "ROLE_EMPLOYEE", "ROLE_RECEPTIONIST", "ROLE_STAFF"));
        if (account.getHotelId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản không thuộc chi nhánh khách sạn.");
        }
    }

    private void requireRole(Account account, Set<String> allowedRoles) {
        boolean allowed = account != null && account.getAuthorities().stream()
                .anyMatch(authority -> allowedRoles.contains(authority.getAuthority()));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản không có quyền thao tác lịch đóng cửa.");
        }
    }

    boolean overlaps(HotelClosureSchedule schedule, BookingDetail detail) {
        if (detail.getCheckinTime() == null || detail.getCheckoutTime() == null
                || detail.getStatus() == BookingStatusType.CANCELLED) {
            return false;
        }
        LocalDate checkin = detail.getCheckinTime().toLocalDate();
        LocalDate checkout = detail.getCheckoutTime().toLocalDate();
        return schedule.getStartDate().isBefore(checkout) && !schedule.getEndDate().isBefore(checkin);
    }

    BigDecimal prorateRoomAmount(BookingDetail detail, LocalDate checkoutDate) {
        if (detail.getCheckinTime() == null || detail.getCheckoutTime() == null) {
            return BigDecimal.ZERO;
        }
        long nights = java.time.temporal.ChronoUnit.DAYS.between(
                detail.getCheckinTime().toLocalDate(), detail.getCheckoutTime().toLocalDate());
        LocalDate checkinDate = detail.getCheckinTime().toLocalDate();
        long nightsBeforeCheckout = java.time.temporal.ChronoUnit.DAYS.between(checkinDate, checkoutDate);
        long usedNights = Math.max(0, Math.min(nights, nightsBeforeCheckout));
        if (nights > 0 && !checkoutDate.isAfter(checkinDate)) {
            usedNights = 1;
        }
        BigDecimal subtotal = BigDecimal.valueOf(detail.getRoomSubTotal() == null ? 0 : detail.getRoomSubTotal());
        return nights <= 0 ? BigDecimal.ZERO
                : subtotal.multiply(BigDecimal.valueOf(usedNights))
                    .divide(BigDecimal.valueOf(nights), 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal usedRoomAmount(BookingDetail detail, LocalDate checkoutDate) {
        List<BookingNightlyPrice> nightlyPrices = detail.getNightlyPrices();
        if (nightlyPrices == null || nightlyPrices.isEmpty()) {
            return prorateRoomAmount(detail, checkoutDate);
        }
        List<BookingNightlyPrice> usedNights = nightlyPrices.stream()
                .filter(price -> price.getStayDate() != null
                        && price.getStayDate().isBefore(checkoutDate))
                .toList();
        if (usedNights.isEmpty()
                && detail.getCheckinTime() != null
                && !checkoutDate.isAfter(detail.getCheckinTime().toLocalDate())) {
            return nightlyPrices.stream()
                    .filter(price -> price.getStayDate() != null)
                    .min(java.util.Comparator.comparing(BookingNightlyPrice::getStayDate))
                    .map(BookingNightlyPrice::getNightTotal)
                    .filter(Objects::nonNull)
                    .orElse(BigDecimal.ZERO);
        }
        return usedNights.stream()
                .map(BookingNightlyPrice::getNightTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    void prorateOrderDiscounts(
            Order order,
            BigDecimal originalRoomTotal,
            BigDecimal usedRoomTotal) {
        if (order == null || originalRoomTotal.signum() <= 0) {
            return;
        }
        BigDecimal retainedRoomTotal = usedRoomTotal.min(originalRoomTotal);
        BigDecimal retainedRoomRatio = retainedRoomTotal.divide(
                originalRoomTotal, 8, java.math.RoundingMode.HALF_UP);
        if (order.getDiscountRoomAmount() != null) {
            order.setDiscountRoomAmount(order.getDiscountRoomAmount().multiply(retainedRoomRatio)
                    .setScale(2, java.math.RoundingMode.HALF_UP));
        }
        BigDecimal originalSubtotal = originalRoomTotal
                .add(order.getServiceTotalAmount() == null ? BigDecimal.ZERO : order.getServiceTotalAmount())
                .add(order.getSurchargeTotalAmount() == null ? BigDecimal.ZERO : order.getSurchargeTotalAmount());
        BigDecimal retainedSubtotal = retainedRoomTotal
                .add(order.getServiceTotalAmount() == null ? BigDecimal.ZERO : order.getServiceTotalAmount())
                .add(order.getSurchargeTotalAmount() == null ? BigDecimal.ZERO : order.getSurchargeTotalAmount());
        if (originalSubtotal.signum() > 0 && order.getDiscountAmountTotal() != null) {
            order.setDiscountAmountTotal(order.getDiscountAmountTotal()
                    .multiply(retainedSubtotal)
                    .divide(originalSubtotal, 2, java.math.RoundingMode.HALF_UP));
        }
    }

    private HotelClosureResponse toResponse(HotelClosureSchedule schedule) {
        return new HotelClosureResponse(schedule.getId(), schedule.getTitle(), schedule.getDescription(),
                schedule.getStartDate(), schedule.getEndDate(), schedule.isAllHotels(),
                schedule.getHotelIds() == null ? List.of() : List.copyOf(schedule.getHotelIds()),
                schedule.getCreatedAt(), schedule.isActive());
    }
}
