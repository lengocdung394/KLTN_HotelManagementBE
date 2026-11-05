package iuh.fit.se.hotelmanagement_be.modular.closure.services;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingNightlyPrice;
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
import java.time.LocalDate;
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
                                    detail.getStatus() == null ? null : detail.getStatus().name()))
                            .toList();
                    String guestName = booking.getCustomer() == null ? "Khách hàng"
                            : booking.getCustomer().getFullName();
                    String action = resolutionRepository.findBySchedule_IdAndBooking_Id(scheduleId, booking.getId())
                            .map(HotelClosureResolution::getAction).orElse(null);
                    return new HotelClosureAffectedBookingResponse(
                            booking.getId(), guestName, booking.getBookingStatus().name(), stays, action);
                })
                .toList();
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
                        : detail.getNightlyPrices().stream()
                            .filter(price -> price.getStayDate() != null
                                    && price.getStayDate().isBefore(checkoutDate))
                            .map(BookingNightlyPrice::getNightTotal)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
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
        long usedNights = Math.max(0, Math.min(nights, java.time.temporal.ChronoUnit.DAYS.between(
                detail.getCheckinTime().toLocalDate(), checkoutDate)));
        BigDecimal subtotal = BigDecimal.valueOf(detail.getRoomSubTotal() == null ? 0 : detail.getRoomSubTotal());
        return nights <= 0 ? BigDecimal.ZERO
                : subtotal.multiply(BigDecimal.valueOf(usedNights))
                    .divide(BigDecimal.valueOf(nights), 2, java.math.RoundingMode.HALF_UP);
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
                schedule.getCreatedAt());
    }
}
