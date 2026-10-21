package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingNightlyPrice;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingServiceDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingManagementRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.*;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingModificationResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingServiceResponseForHotel;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.ExtraFeeBreakdownResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.PromotionDiscountResult;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingManagementService;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.impl.BookingSocketEmitter;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentStatus;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.CustomerPromotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.Promotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.CustomerPromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.services.PromotionUsageService;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.PromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomSeasonalRate;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomSeasonalRateRepository;
import iuh.fit.se.hotelmanagement_be.modular.service.repositories.ServiceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingServiceManagementServiceImpl implements BookingManagementService {
    ServiceRepository serviceRepository;
    RoomRepository roomRepository;
    BookingManagementRepository bookingManagementRepository;
    BookingRepository bookingRepository;
    EmployeeRepository employeeRepository;
    BranchRoomPolicyRepository branchRoomPolicyRepository;
    RoomSeasonalRateRepository roomSeasonalRateRepository;
    RoomPricingCalculator roomPricingCalculator;

    BookingServiceImpl bookingServiceImpl;
    PromotionRepository promotionRepository;

    CustomerPromotionRepository customerPromotionRepository;
    PromotionUsageService promotionUsageService;
    BookingSocketEmitter bookingSocketEmitter;

    @Override
    @Transactional
    public BookingServiceResponseForHotel confirmServiceUsed(String bookingId, String serviceDetailId, Account account) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if (account.getHotelId() == null || booking.getHotel() == null
                || !account.getHotelId().equals(booking.getHotel().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_BRANCH_ACCESS);
        }
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }

        BookingDetail detail = booking.getBookingDetails().stream()
                .filter(item -> item.getBookingServiceDetails() != null
                        && item.getBookingServiceDetails().stream()
                        .anyMatch(service -> service.getId().equals(serviceDetailId)))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_SERVICE_DETAIL_NOT_FOUND));
        if (detail.getStatus() == BookingStatusType.CANCELLED) {
            throw new AppException(ErrorCode.CANNOT_CANCEL_SERVICE_IN_CANCELLED_ROOM);
        }

        BookingServiceDetail serviceDetail = detail.getBookingServiceDetails().stream()
                .filter(item -> item.getId().equals(serviceDetailId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_SERVICE_DETAIL_NOT_FOUND));
        if (Boolean.TRUE.equals(serviceDetail.getCancelled())) {
            throw new AppException(ErrorCode.BOOKING_SERVICE_DETAIL_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(serviceDetail.getUsed())) {
            throw new AppException(ErrorCode.SERVICE_ALREADY_USED);
        }

        serviceDetail.setUsed(true);
        serviceDetail.setUsedAt(LocalDateTime.now());
        serviceDetail.setUsedBy(account.getEmployee() == null ? account.getEmail() : account.getEmployee().getFullName());

        return BookingServiceResponseForHotel.builder()
                .bookingServiceDetailId(serviceDetail.getId())
                .serviceId(serviceDetail.getService().getId())
                .name(serviceDetail.getName())
                .quantity(serviceDetail.getQuantity())
                .price(serviceDetail.getPrice())
                .subTotal(serviceDetail.getPrice() == null
                        ? 0.0
                        : serviceDetail.getPrice() * serviceDetail.getQuantity())
                .usedAt(serviceDetail.getUsedAt())
                .used(serviceDetail.getUsed())
                .usedBy(serviceDetail.getUsedBy())
                .cancelled(serviceDetail.getCancelled())
                .cancelledAt(serviceDetail.getCancelledAt())
                .build();
    }


    // Thêm phòng + kèm theo dịch vụ
    @Override
    public BigDecimal processRoomAdditions(Booking booking, List<NewRoomRequest> roomsToAdd) {
        if (roomsToAdd == null || roomsToAdd.isEmpty()) {
            log.debug("No rooms to add for booking ID: {}", booking.getId());
            return BigDecimal.ZERO;
        }

        log.info("Processing room additions. Total rooms to add: {} for booking ID: {}", roomsToAdd.size(), booking.getId());
        BigDecimal totalRoomAndServiceAdded = BigDecimal.ZERO;
        // Lay tu booking
        Long hotelId = booking.getHotel().getId();

        for (NewRoomRequest roomReq : roomsToAdd) {
            log.info("Adding room ID: {} from {} to {}, Adults: {}, Children: {}",
                    roomReq.getRoomId(), roomReq.getCheckInTime(), roomReq.getCheckOutTime(),
                    roomReq.getNumAdults(), roomReq.getNumChildren());

            Room room = roomRepository.findById(roomReq.getRoomId())
                    .orElseThrow(() -> {
                        log.error("Room not found with ID: {}", roomReq.getRoomId());
                        return new AppException(ErrorCode.ROOM_NOT_FOUND);
                    });
            if (room.getRoomStatus() == RoomStatus.MAINTENANCE
                    || room.getRoomStatus() == RoomStatus.CLEANING) {
                throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE_FOR_BOOKING);
            }

            LocalDate startDate = roomReq.getCheckInTime().toLocalDate();
            LocalDate endDate = roomReq.getCheckOutTime().toLocalDate();
            if (!endDate.isAfter(startDate)) {
                throw new AppException(ErrorCode.INVALID_CHECKOUT_DATE);
            }
            BranchRoomPolicy policy = branchRoomPolicyRepository.findByHotelIdAndRoomType(hotelId, room.getRoomType());
            List<BookingNightlyPrice> nightlyPrices = calculateNightlyPrices(
                    hotelId, room, startDate, endDate, roomReq.getNumAdults(), roomReq.getNumChildren(), policy);
            BigDecimal roomSubTotal = totalNightlyPrice(nightlyPrices);
            double sampleBasePricePerNight = nightlyPrices.stream()
                    .map(BookingNightlyPrice::getRoomBasePrice)
                    .filter(price -> price != null)
                    .mapToDouble(BigDecimal::doubleValue)
                    .average()
                    .orElse(0.0);
            ExtraFeeBreakdownResponse extraFeeBreakdown = roomPricingCalculator.calculateExtraFeeBreakdown(
                    policy, roomReq.getNumAdults(), roomReq.getNumChildren());

            // 1. Khởi tạo BookingDetail mới
            BookingDetail newDetail = BookingDetail.builder()
                    .room(room)
                    .checkinTime(roomReq.getCheckInTime())
                    .checkoutTime(roomReq.getCheckOutTime())
                    .numAdults(roomReq.getNumAdults())
                    .numChildren(roomReq.getNumChildren())
                    .baseRoomPricePerNight(sampleBasePricePerNight)
                    .extraAdultFeePerNight(extraFeeBreakdown.getAdultExtraFee())
                    .extraChildFeePerNight(extraFeeBreakdown.getChildExtraFee())
                    .roomSubTotal(roomSubTotal.doubleValue())
                    .totalPrice(roomSubTotal.doubleValue())
                    .status(BookingStatusType.PENDING)
                    .build();
            nightlyPrices.forEach(newDetail::addNightlyPrice);

            // 2. XỬ LÝ DỊCH VỤ ĐI KÈM NGAY KHI THÊM PHÒNG (NẾU CÓ)
            BigDecimal roomServiceSubTotal = BigDecimal.ZERO;
            if (roomReq.getServiceRequests() != null && !roomReq.getServiceRequests().isEmpty()) {
                log.info("Processing {} accompanying services for new room ID: {}", roomReq.getServiceRequests().size(), room.getId());
                List<BookingServiceDetail> serviceDetails = new ArrayList<>();
                for (NewServiceRequest sReq : roomReq.getServiceRequests()) {
                    iuh.fit.se.hotelmanagement_be.modular.service.entities.Service catalogService = serviceRepository.findById(sReq.getServiceId())
                            .orElseThrow(() -> {
                                log.error("Service not found with ID: {}", sReq.getServiceId());
                                return new AppException(ErrorCode.SERVICE_NOT_FOUND);
                            });

                    BookingServiceDetail serviceDetail = BookingServiceDetail.builder()
                            .bookingDetail(newDetail)
                            .service(catalogService)
                            .name(catalogService.getName())
                            .price(catalogService.getPrice())
                            .quantity(sReq.getQuantity())
                            .isPaid(false)
                            .cancelled(false)
                            .build();

                    serviceDetails.add(serviceDetail);

                    double serviceItemTotal = catalogService.getPrice() * sReq.getQuantity();
                    roomServiceSubTotal = roomServiceSubTotal.add(BigDecimal.valueOf(serviceItemTotal));
                    log.debug("Added service: {}, Quantity: {}, Total: {}", catalogService.getName(), sReq.getQuantity(), serviceItemTotal);
                }
                newDetail.setBookingServiceDetails(serviceDetails);
                newDetail.setTotalPrice(roomSubTotal.add(roomServiceSubTotal).doubleValue());
            }

            booking.addBookingDetail(newDetail);
            totalRoomAndServiceAdded = totalRoomAndServiceAdded.add(roomSubTotal).add(roomServiceSubTotal);
        }

        log.info("Finished adding rooms. Total room and service amount added: {}", totalRoomAndServiceAdded);
        return totalRoomAndServiceAdded;
    }

    @Override
    public BigDecimal processRoomUpdates(Booking booking, List<RoomUpdateRequest> updates) {
        if (updates == null || updates.isEmpty()) {
            log.debug("No room updates requested for booking ID: {}", booking.getId());
            return BigDecimal.ZERO;
        }

        log.info("Processing room updates (room change, date/guest updates). Total requests: {} for booking ID: {}", updates.size(), booking.getId());
        BigDecimal totalRoomPriceChange = BigDecimal.ZERO;
        Long hotelId = booking.getHotel().getId();

        for (RoomUpdateRequest req : updates) {
            // 1. Tìm BookingDetail cần cập nhật
            BookingDetail targetDetail = booking.getBookingDetails().stream()
                    .filter(d -> d.getId().equals(req.getBookingDetailId()))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error("BookingDetail not found with ID: {}", req.getBookingDetailId());
                        return new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND);
                    });

            if (targetDetail.getStatus() == BookingStatusType.CANCELLED) {
                log.warn("Attempted to update a cancelled BookingDetail ID: {}", targetDetail.getId());
                throw new AppException(ErrorCode.CANNOT_UPDATE_CANCELLED_ROOM);
            }

            Room originalRoom = targetDetail.getRoom();
            Integer originalAdults = targetDetail.getNumAdults();
            Integer originalChildren = targetDetail.getNumChildren();
            BigDecimal oldRoomSubTotal = BigDecimal.valueOf(
                    targetDetail.getRoomSubTotal() != null ? targetDetail.getRoomSubTotal() : 0.0);
            ensureLegacyNightlyPrices(targetDetail);

            boolean roomChanged = req.getNewRoomId() != null && !req.getNewRoomId().equals(originalRoom.getId());
            if (roomChanged) {
                Room newRoom = roomRepository.findById(req.getNewRoomId())
                        .orElseThrow(() -> {
                            log.error("New Room not found with ID: {}", req.getNewRoomId());
                            return new AppException(ErrorCode.ROOM_NOT_FOUND);
                        });
                if (newRoom.getRoomStatus() == RoomStatus.MAINTENANCE
                        || newRoom.getRoomStatus() == RoomStatus.CLEANING) {
                    throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE_FOR_BOOKING);
                }
                targetDetail.setRoom(newRoom);
                log.info("Updated room ID to {} for BookingDetail ID: {}", newRoom.getId(), targetDetail.getId());
            }

            LocalDateTime checkInToUse = req.getNewCheckInTime() != null ? req.getNewCheckInTime() : targetDetail.getCheckinTime();
            LocalDateTime checkOutToUse = req.getNewCheckoutTime() != null ? req.getNewCheckoutTime() : targetDetail.getCheckoutTime();
            if (checkInToUse == null || checkOutToUse == null
                    || !checkOutToUse.toLocalDate().isAfter(checkInToUse.toLocalDate())) {
                throw new AppException(ErrorCode.INVALID_CHECKOUT_DATE);
            }

            targetDetail.setCheckinTime(checkInToUse);
            targetDetail.setCheckoutTime(checkOutToUse);

            if (req.getNumAdults() != null) {
                targetDetail.setNumAdults(req.getNumAdults());
            }
            if (req.getNumChildren() != null) {
                targetDetail.setNumChildren(req.getNumChildren());
            }
            if (req.getNumInfants() != null) {
                targetDetail.setNumInfants(req.getNumInfants());
            }

            Room roomToUse = targetDetail.getRoom();
            BranchRoomPolicy policy = branchRoomPolicyRepository.findByHotelIdAndRoomType(hotelId, roomToUse.getRoomType());
            LocalDate newCheckInDate = checkInToUse.toLocalDate();
            LocalDate newCheckOutDate = checkOutToUse.toLocalDate();
            List<BookingNightlyPrice> nightlyPrices = targetDetail.getNightlyPrices();
            boolean guestCountChanged = !java.util.Objects.equals(originalAdults, targetDetail.getNumAdults())
                    || !java.util.Objects.equals(originalChildren, targetDetail.getNumChildren());
            ExtraFeeBreakdownResponse updatedFees = null;
            boolean hasSnapshotGuestPricing = !nightlyPrices.isEmpty()
                    && nightlyPrices.stream().allMatch(price -> price.getExtraAdultUnitPrice() != null
                    && price.getExtraChildUnitPrice() != null
                    && price.getStandardCapacity() != null
                    && price.getMaxExtraGuests() != null);
            if (roomChanged || (guestCountChanged && !hasSnapshotGuestPricing)) {
                updatedFees = roomPricingCalculator.calculateExtraFeeBreakdown(
                        policy, targetDetail.getNumAdults(), targetDetail.getNumChildren());
                targetDetail.setExtraAdultFeePerNight(updatedFees.getAdultExtraFee());
                targetDetail.setExtraChildFeePerNight(updatedFees.getChildExtraFee());
            } else if (guestCountChanged) {
                updatedFees = calculateSnapshotExtraFees(
                        targetDetail.getNumAdults(), targetDetail.getNumChildren(), nightlyPrices.get(0));
                targetDetail.setExtraAdultFeePerNight(updatedFees.getAdultExtraFee());
                targetDetail.setExtraChildFeePerNight(updatedFees.getChildExtraFee());
            }

            if (roomChanged) {
                nightlyPrices.clear();
                calculateNightlyPrices(
                        hotelId, roomToUse, newCheckInDate, newCheckOutDate,
                        targetDetail.getNumAdults(), targetDetail.getNumChildren(), policy
                ).forEach(targetDetail::addNightlyPrice);
            } else {
                nightlyPrices.removeIf(price -> price.getStayDate().isBefore(newCheckInDate)
                        || !price.getStayDate().isBefore(newCheckOutDate));

                if (guestCountChanged) {
                    for (BookingNightlyPrice nightlyPrice : nightlyPrices) {
                        BigDecimal oldAdultFee = nightlyPrice.getExtraAdultFee() != null
                                ? nightlyPrice.getExtraAdultFee()
                                : BigDecimal.valueOf(targetDetail.getExtraAdultFeePerNight() != null ? targetDetail.getExtraAdultFeePerNight() : 0.0);
                        BigDecimal oldChildFee = nightlyPrice.getExtraChildFee() != null
                                ? nightlyPrice.getExtraChildFee()
                                : BigDecimal.valueOf(targetDetail.getExtraChildFeePerNight() != null ? targetDetail.getExtraChildFeePerNight() : 0.0);
                        BigDecimal basePrice = nightlyPrice.getRoomBasePrice() != null
                                ? nightlyPrice.getRoomBasePrice()
                                : nightlyPrice.getNightTotal().subtract(oldAdultFee).subtract(oldChildFee);
                        ExtraFeeBreakdownResponse nightlyFees = hasSnapshotGuestPricing
                                ? calculateSnapshotExtraFees(
                                        targetDetail.getNumAdults(), targetDetail.getNumChildren(), nightlyPrice)
                                : updatedFees;
                        BigDecimal adultFee = BigDecimal.valueOf(nightlyFees.getAdultExtraFee());
                        BigDecimal childFee = BigDecimal.valueOf(nightlyFees.getChildExtraFee());
                        nightlyPrice.setRoomBasePrice(basePrice);
                        nightlyPrice.setExtraAdultFee(adultFee);
                        nightlyPrice.setExtraChildFee(childFee);
                        nightlyPrice.setNightTotal(basePrice.add(adultFee).add(childFee).setScale(2, RoundingMode.HALF_UP));
                    }
                }

                List<BookingNightlyPrice> existingPrices = new ArrayList<>(nightlyPrices);
                for (LocalDate stayDate = newCheckInDate; stayDate.isBefore(newCheckOutDate); stayDate = stayDate.plusDays(1)) {
                    LocalDate dateToAdd = stayDate;
                    boolean priceExists = existingPrices.stream().anyMatch(price -> price.getStayDate().equals(dateToAdd));
                    if (!priceExists) {
                        calculateNightlyPrices(
                                hotelId, roomToUse, stayDate, stayDate.plusDays(1),
                                targetDetail.getNumAdults(), targetDetail.getNumChildren(), policy
                        ).forEach(targetDetail::addNightlyPrice);
                    }
                }
            }

            BigDecimal newRoomSubTotal = totalNightlyPrice(nightlyPrices);
            targetDetail.setRoomSubTotal(newRoomSubTotal.doubleValue());
            targetDetail.setBaseRoomPricePerNight(nightlyPrices.stream()
                    .map(BookingNightlyPrice::getRoomBasePrice)
                    .filter(price -> price != null)
                    .mapToDouble(BigDecimal::doubleValue)
                    .average()
                    .orElse(targetDetail.getBaseRoomPricePerNight() != null ? targetDetail.getBaseRoomPricePerNight() : 0.0));
            totalRoomPriceChange = totalRoomPriceChange.subtract(oldRoomSubTotal).add(newRoomSubTotal);

            // Tính lại tổng tiền riêng của BookingDetail (Tiền phòng mới + Tiền dịch vụ không bị hủy)
            double currentServiceSub = targetDetail.getBookingServiceDetails() == null ? 0.0 :
                    targetDetail.getBookingServiceDetails().stream()
                            .filter(sd -> !Boolean.TRUE.equals(sd.getCancelled()))
                            .mapToDouble(sd -> (sd.getPrice() != null ? sd.getPrice() : 0.0) * sd.getQuantity())
                            .sum();

            targetDetail.setTotalPrice(newRoomSubTotal.doubleValue() + currentServiceSub);
            log.info("Successfully updated BookingDetail ID: {}. New Room Subtotal: {}", targetDetail.getId(), newRoomSubTotal);
        }

        return totalRoomPriceChange;
    }

    // Hàm Nhạc Trưởng
    @Override
    @Transactional
    public BookingModificationResponse modifyBooking(String bookingId, BookingModificationRequest request) {

        log.info("==> START modifying booking ID: {} by Employee ID: {}", bookingId, request.getEmployeeId());

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> {
                    log.error("Booking not found with ID: {}", bookingId);
                    return new AppException(ErrorCode.BOOKING_NOT_FOUND);
                });

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            log.warn("Attempted to modify an already cancelled booking ID: {}", bookingId);
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }
        boolean allRoomsCheckedOut = booking.getBookingDetails() != null
                && booking.getBookingDetails().stream()
                .filter(detail -> detail.getStatus() != BookingStatusType.CANCELLED)
                .anyMatch(detail -> detail.getStatus() == BookingStatusType.CHECKED_OUT)
                && booking.getBookingDetails().stream()
                .filter(detail -> detail.getStatus() != BookingStatusType.CANCELLED)
                .allMatch(detail -> detail.getStatus() == BookingStatusType.CHECKED_OUT);
        if (booking.getBookingStatus() == BookingStatus.COMPLETED || allRoomsCheckedOut) {
            log.warn("Attempted to modify a checked-out booking ID: {}", bookingId);
            throw new AppException(ErrorCode.BOOKING_ALREADY_COMPLETED);
        }

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> {
                    log.error("Employee not found with ID: {}", request.getEmployeeId());
                    return new AppException(ErrorCode.EMPLOYEE_NOT_FOUND);
                });

        String operatorName = employee.getFullName();
        Order order = booking.getOrder();

        BigDecimal roomPriceChange = BigDecimal.ZERO;
        BigDecimal servicePriceChange = BigDecimal.ZERO;

        // 1. Xử lý hủy phòng hàng loạt - huy luon dich vu
        roomPriceChange = roomPriceChange.add(processCancellations(booking, request.getBookingDetailIdsToCancel(), operatorName));

        // 2. Xử lý hủy dịch vụ lẻ
        servicePriceChange = servicePriceChange.add(processServiceCancellations(booking, request.getServicesToCancel()));

        // 3. Xử lý thêm phòng mới (kèm dịch vụ) hàng loạt
        roomPriceChange = roomPriceChange.add(processRoomAdditions(booking, request.getRoomsToAdd()));

        // 4. Xử lý cập nhật ngày checkin/checkout hàng loạt co the la doi phong kem theo so luong nguoi
        roomPriceChange = roomPriceChange.add(processRoomUpdates(booking, request.getRoomsToChange()));

        // 5. Xử lý thêm dịch vụ phát sinh cho phòng cũ
        servicePriceChange = servicePriceChange.add(processServicesForExistingRooms(booking, request.getServicesToAddForExistingRooms()));

        // 6. Xử lý cập nhật/giảm số lượng dịch vụ theo phòng
        servicePriceChange = servicePriceChange.add(processServiceQuantityUpdates(booking, request.getServiceQuantityUpdates()));

        // ==========================================
        // 6. TÍNH TOÁN TỔNG TIỀN PHÒNG VÀ DỊCH VỤ SAU KHI MODIFY
        // ==========================================
        BigDecimal totalRoom = BigDecimal.ZERO;
        BigDecimal totalService = BigDecimal.ZERO;
        BigDecimal totalSurcharge = BigDecimal.ZERO;

        for (BookingDetail detail : booking.getBookingDetails()) {
            if (detail.getStatus() == BookingStatusType.CANCELLED) {
                detail.setTotalPrice(0.0);
                continue;
            }

            BigDecimal earlyCheckInFee = detail.getEarlyCheckInFee() != null
                    ? detail.getEarlyCheckInFee()
                    : BigDecimal.ZERO;
            BigDecimal lateCheckOutFee = detail.getLateCheckOutFee() != null
                    ? detail.getLateCheckOutFee()
                    : BigDecimal.ZERO;
            BigDecimal otherSurcharges = detail.getOtherSurcharges() != null
                    ? detail.getOtherSurcharges()
                    : BigDecimal.ZERO;
            totalSurcharge = totalSurcharge
                    .add(earlyCheckInFee)
                    .add(lateCheckOutFee)
                    .add(otherSurcharges);

            double roomSub = detail.getRoomSubTotal() != null ? detail.getRoomSubTotal() : 0.0;

            // Tính tổng tiền dịch vụ KHÔNG BỊ HỦY của phòng này
            double serviceSub = 0.0;
            if (detail.getBookingServiceDetails() != null) {
                serviceSub = detail.getBookingServiceDetails().stream()
                        .filter(sd -> !Boolean.TRUE.equals(sd.getCancelled()))
                        .mapToDouble(sd -> (sd.getPrice() != null ? sd.getPrice() : 0.0) * sd.getQuantity())
                        .sum();
            }

            // Cập nhật totalPrice cho từng BookingDetail (Tiền phòng + Tiền dịch vụ phòng đó)
            detail.setTotalPrice(roomSub + serviceSub);

            // Cộng dồn vào tổng chung của Order
            totalRoom = totalRoom.add(BigDecimal.valueOf(roomSub));
            totalService = totalService.add(BigDecimal.valueOf(serviceSub));
        }

        // 7. KIỂM TRA LẠI KHUYẾN MÃI (REAL-TIME VALIDATION) & TÍNH TOÁN LẠI

        PromotionDiscountResult discountResult = applyOrReevaluatePromotion(request, order, totalRoom, totalService);

        BigDecimal discountRoom = discountResult.getDiscountRoomAmount() != null ? discountResult.getDiscountRoomAmount() : BigDecimal.ZERO;
        BigDecimal discountService = discountResult.getDiscountServiceAmount() != null ? discountResult.getDiscountServiceAmount() : BigDecimal.ZERO;
        BigDecimal discountTotalOrder = discountResult.getDiscountAmountTotal() != null ? discountResult.getDiscountAmountTotal() : BigDecimal.ZERO;

        // Tổng tất cả tiền giảm từ mọi scope (ROOM, SERVICE, TOTAL)
        BigDecimal totalDiscount = discountRoom.add(discountService).add(discountTotalOrder);
        booking.setApplyAmount(totalDiscount);

        // 8. CHỐT TỔNG TIỀN VÀ TRẠNG THÁI ORDER
        BigDecimal currentTotalAmountBeforeDiscount = totalRoom.add(totalService).add(totalSurcharge);
        BigDecimal newTotalAmount = currentTotalAmountBeforeDiscount.subtract(totalDiscount); // Trừ chuẩn tổng tiền giảm ở đây!
        BigDecimal oldTotalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal totalOrderChange = newTotalAmount.subtract(oldTotalAmount);

        BigDecimal paidAmount = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal remainingAmount = newTotalAmount.subtract(paidAmount).max(BigDecimal.ZERO);

        // Gán lại các giá trị tổng và khuyến mãi đầy đủ vào Order
        order.setRoomTotalAmount(totalRoom);
        order.setServiceTotalAmount(totalService);
        order.setSurchargeTotalAmount(totalSurcharge);

        order.setDiscountAmountTotal(discountTotalOrder);
        order.setDiscountRoomAmount(discountRoom);
        order.setDiscountServiceAmount(discountService);

        order.setTotalAmount(newTotalAmount);        // Lúc này newTotalAmount đã được trừ 10 đồng chính xác
        order.setRemainingAmount(remainingAmount);   // Remaining amount sẽ tự động chuẩn (VD: 300030.00)
        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            // Nếu đã trả đủ hoặc dư (tiền thừa) -> Trạng thái là ĐÃ THANH TOÁN
            order.setPaymentStatus(PaymentStatus.PAID);
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Nếu đã trả một phần (có cọc/trả trước) nhưng vẫn còn thiếu -> TRẢ THANH TOÁN MỘT PHẦN
            order.setPaymentStatus(PaymentStatus.PARTIAL);
        } else {
            // Nếu chưa trả đồng nào -> CHƯA THANH TOÁN
            order.setPaymentStatus(PaymentStatus.UNPAID);
        }

        log.info("Booking modification summary for Booking ID: {} -> Total Room: {}, Total Service: {}, Discount: {}, Old total: {}, New total: {}",
                bookingId, totalRoom, totalService, oldTotalAmount, newTotalAmount);

        bookingManagementRepository.save(booking);
        Long hotelId = booking.getHotel() != null ? booking.getHotel().getId() : null;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                bookingSocketEmitter.emitBookingUpdated(
                        hotelId,
                        booking,
                        request,
                        oldTotalAmount,
                        newTotalAmount
                );
            }
        });
        log.info("<== FINISHED modifying booking ID: {} successfully.", bookingId);

        return BookingModificationResponse.builder()
                .bookingId(bookingId)
                .oldTotalAmount(oldTotalAmount)
                .newTotalAmount(newTotalAmount)
                .totalChange(totalOrderChange)
                .build();
    }


    // Hủy nhiều phòng (đã kèm dịch vụ)
    @Override
    public BigDecimal processCancellations(Booking booking, List<String> detailIdsToCancel, String operatorName) {
        if (detailIdsToCancel == null || detailIdsToCancel.isEmpty()) {
            log.debug("No room cancellations requested for booking ID: {}", booking.getId());
            return BigDecimal.ZERO;
        }

        log.info("Processing room cancellations. Total rooms to cancel: {} by operator: {}", detailIdsToCancel.size(), operatorName);
        BigDecimal roomPriceReduced = BigDecimal.ZERO;

        for (String detailId : detailIdsToCancel) {
            BookingDetail targetDetail = booking.getBookingDetails().stream()
                    .filter(d -> d.getId().equals(detailId))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error("BookingDetail not found for cancellation with ID: {}", detailId);
                        return new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND);
                    });

            if (targetDetail.getStatus() == BookingStatusType.CANCELLED) {
                log.debug("BookingDetail ID: {} is already cancelled. Skipping.", detailId);
                continue;
            }
            if (targetDetail.getBookingServiceDetails() != null && targetDetail.getBookingServiceDetails().stream()
                    .anyMatch(service -> Boolean.TRUE.equals(service.getUsed())
                            && !Boolean.TRUE.equals(service.getCancelled()))) {
                throw new AppException(ErrorCode.SERVICE_ALREADY_USED);
            }

            targetDetail.setStatus(BookingStatusType.CANCELLED);
            targetDetail.setCancelledAt(LocalDateTime.now());
            targetDetail.setCancelledBy(operatorName);

            if (targetDetail.getRoomSubTotal() != null) {
                roomPriceReduced = roomPriceReduced.subtract(BigDecimal.valueOf(targetDetail.getRoomSubTotal()));
                log.debug("Reduced room subtotal: {} for cancelled room ID: {}", targetDetail.getRoomSubTotal(), detailId);
            }

            if (targetDetail.getBookingServiceDetails() != null) {
                for (BookingServiceDetail sd : targetDetail.getBookingServiceDetails()) {
                    if (!Boolean.TRUE.equals(sd.getCancelled())) {
                        sd.setCancelled(true);
                        sd.setCancelledAt(LocalDateTime.now());
                        log.debug("Cancelled associated service ID: {} due to room cancellation", sd.getId());
                    }
                }
            }
        }

        log.info("Total room price reduced from cancellations: {}", roomPriceReduced);
        return roomPriceReduced;
    }

    // Ham them dich vu cho phongf
    @Override
    public BigDecimal processServicesForExistingRooms(Booking booking, List<RoomServiceAdditionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            log.debug("No additional services to add for existing rooms in booking ID: {}", booking.getId());
            return BigDecimal.ZERO;
        }

        log.info("Processing additional services for existing rooms. Total items: {} for booking ID: {}", requests.size(), booking.getId());
        BigDecimal totalServicePriceAdded = BigDecimal.ZERO;

        for (RoomServiceAdditionRequest req : requests) {
            BookingDetail targetDetail = booking.getBookingDetails().stream()
                    .filter(d -> d.getId().equals(req.getBookingDetailId()))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error("BookingDetail not found with ID: {} when adding services", req.getBookingDetailId());
                        return new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND);
                    });

            if (targetDetail.getStatus() == BookingStatusType.CANCELLED) {
                log.warn("Attempted to add services to a cancelled BookingDetail ID: {}", targetDetail.getId());
                throw new AppException(ErrorCode.CANNOT_ADD_SERVICE_TO_CANCELLED_ROOM);
            }

            if (req.getServices() == null || req.getServices().isEmpty()) {
                continue;
            }

            // Đảm bảo list service details không bị null
            if (targetDetail.getBookingServiceDetails() == null) {
                targetDetail.setBookingServiceDetails(new ArrayList<>());
            }

            for (NewServiceRequest sReq : req.getServices()) {
                iuh.fit.se.hotelmanagement_be.modular.service.entities.Service catalogService = serviceRepository.findById(sReq.getServiceId())
                        .orElseThrow(() -> {
                            log.error("Service not found with ID: {}", sReq.getServiceId());
                            return new AppException(ErrorCode.SERVICE_NOT_FOUND);
                        });

                // 1. Kiểm tra xem dịch vụ này ĐÃ TỒN TẠI TRONG PHÒNG NÀY CHƯA (và chưa bị hủy)
                BookingServiceDetail existingServiceDetail = targetDetail.getBookingServiceDetails().stream()
                        .filter(sd -> sd.getService().getId().equals(sReq.getServiceId())
                                && !Boolean.TRUE.equals(sd.getCancelled())
                                && !Boolean.TRUE.equals(sd.getUsed()))
                        .findFirst()
                        .orElse(null);

                double itemTotal = 0.0;
                double price = catalogService.getPrice() != null ? catalogService.getPrice() : 0.0;

                if (existingServiceDetail != null) {
                    // TRƯỜNG HỢP A: Dịch vụ đã tồn tại -> Cộng dồn số lượng lên
                    int oldQty = existingServiceDetail.getQuantity();
                    int addedQty = sReq.getQuantity();
                    int newQty = oldQty + addedQty;

                    existingServiceDetail.setQuantity(newQty);
                    itemTotal = price * addedQty;
                    log.info("Service ID: {} already exists in BookingDetail ID: {}. Increased quantity from {} to {}",
                            sReq.getServiceId(), targetDetail.getId(), oldQty, newQty);
                } else {
                    // TRƯỜNG HỢP B: Lần đầu thêm dịch vụ này vào phòng -> Tạo mới hoàn toàn
                    BookingServiceDetail serviceDetail = BookingServiceDetail.builder()
                            .bookingDetail(targetDetail)
                            .service(catalogService)
                            .name(catalogService.getName())
                            .price(price)
                            .quantity(sReq.getQuantity())
                            .isPaid(false)
                            .cancelled(false)
                            .build();

                    targetDetail.getBookingServiceDetails().add(serviceDetail);

                    itemTotal = price * sReq.getQuantity();
                    log.info("Added new service '{}' (x{}) to BookingDetail ID: {}. Amount added: {}",
                            catalogService.getName(), sReq.getQuantity(), targetDetail.getId(), itemTotal);
                }

                totalServicePriceAdded = totalServicePriceAdded.add(BigDecimal.valueOf(itemTotal));
            }

            // =========================================================================
            // TÍNH LẠI TOÀN BỘ TIỀN CHO PHÒNG NÀY (TIỀN PHÒNG + TỔNG TẤT CẢ DỊCH VỤ KHÔNG HỦY)
            // =========================================================================
            double roomSub = targetDetail.getRoomSubTotal() != null ? targetDetail.getRoomSubTotal() : 0.0;

            double updatedServiceSubTotal = targetDetail.getBookingServiceDetails().stream()
                    .filter(sd -> !Boolean.TRUE.equals(sd.getCancelled()))
                    .mapToDouble(sd -> (sd.getPrice() != null ? sd.getPrice() : 0.0) * sd.getQuantity())
                    .sum();

            targetDetail.setTotalPrice(roomSub + updatedServiceSubTotal);
        }

        return totalServicePriceAdded;
    }

    private BigDecimal processServiceCancellations(Booking booking, List<ServiceCancellationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal servicePriceChange = BigDecimal.ZERO;
        for (ServiceCancellationRequest request : requests) {
            BookingDetail targetDetail = booking.getBookingDetails().stream()
                    .filter(detail -> detail.getId().equals(request.getBookingDetailId()))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND));

            if (targetDetail.getStatus() == BookingStatusType.CANCELLED) {
                continue;
            }
            if (request.getServiceDetailIds() == null || request.getServiceDetailIds().isEmpty()) {
                continue;
            }

            for (String serviceDetailId : request.getServiceDetailIds()) {
                BookingServiceDetail serviceDetail = targetDetail.getBookingServiceDetails().stream()
                        .filter(item -> item.getId().equals(serviceDetailId))
                        .findFirst()
                        .orElseThrow(() -> new AppException(ErrorCode.BOOKING_SERVICE_DETAIL_NOT_FOUND));

                if (Boolean.TRUE.equals(serviceDetail.getCancelled())) {
                    continue;
                }
                if (Boolean.TRUE.equals(serviceDetail.getUsed())) {
                    throw new AppException(ErrorCode.SERVICE_ALREADY_USED);
                }

                serviceDetail.setCancelled(true);
                serviceDetail.setCancelledAt(LocalDateTime.now());

                double price = serviceDetail.getPrice() != null ? serviceDetail.getPrice() : 0.0;
                int quantity = serviceDetail.getQuantity();
                servicePriceChange = servicePriceChange.subtract(BigDecimal.valueOf(price * quantity));
            }
        }

        return servicePriceChange;
    }

    // Ham giam so luong dich vu
    @Override
    public BigDecimal processServiceQuantityUpdates(Booking booking, List<UpdateServiceQuantityRequest> quantityUpdates) {
        if (quantityUpdates == null || quantityUpdates.isEmpty()) {
            log.debug("No service quantity updates requested for booking ID: {}", booking.getId());
            return BigDecimal.ZERO;
        }

        log.info("Processing service quantity updates for booking ID: {}", booking.getId());
        BigDecimal servicePriceChange = BigDecimal.ZERO;

        for (UpdateServiceQuantityRequest updateReq : quantityUpdates) {
            // 1. Tìm đúng phòng (BookingDetail) theo ID
            BookingDetail targetDetail = booking.getBookingDetails().stream()
                    .filter(d -> d.getId().equals(updateReq.getBookingDetailId()))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error("BookingDetail not found with ID: {}", updateReq.getBookingDetailId());
                        return new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND);
                    });

            if (targetDetail.getStatus() == BookingStatusType.CANCELLED) {
                log.warn("Attempted to update service quantity in a cancelled BookingDetail ID: {}", targetDetail.getId());
                throw new AppException(ErrorCode.CANNOT_CANCEL_SERVICE_IN_CANCELLED_ROOM);
            }

            if (updateReq.getServices() == null || updateReq.getServices().isEmpty()) {
                continue;
            }

            if (targetDetail.getBookingServiceDetails() == null) {
                targetDetail.setBookingServiceDetails(new ArrayList<>());
            }

            for (ServiceQuantityItem sItem : updateReq.getServices()) {
                BookingServiceDetail targetService;
                if (sItem.getBookingServiceDetailId() != null && !sItem.getBookingServiceDetailId().isBlank()) {
                    targetService = targetDetail.getBookingServiceDetails().stream()
                            .filter(sd -> sd.getId().equals(sItem.getBookingServiceDetailId())
                                    && sd.getService().getId().equals(sItem.getServiceId()))
                            .findFirst()
                            .orElseThrow(() -> new AppException(ErrorCode.BOOKING_SERVICE_DETAIL_NOT_FOUND));
                    if (Boolean.TRUE.equals(targetService.getCancelled())) {
                        throw new AppException(ErrorCode.BOOKING_SERVICE_DETAIL_NOT_FOUND);
                    }
                    if (Boolean.TRUE.equals(targetService.getUsed())) {
                        throw new AppException(ErrorCode.SERVICE_ALREADY_USED);
                    }
                } else {
                    targetService = targetDetail.getBookingServiceDetails().stream()
                            .filter(sd -> sd.getService().getId().equals(sItem.getServiceId())
                                    && !Boolean.TRUE.equals(sd.getCancelled())
                                    && !Boolean.TRUE.equals(sd.getUsed()))
                            .findFirst()
                            .orElse(null);
                }

                int newQuantity = sItem.getQuantity();

                if (targetService == null) {
                    BookingServiceDetail usedService = targetDetail.getBookingServiceDetails().stream()
                            .filter(sd -> sd.getService().getId().equals(sItem.getServiceId())
                                    && !Boolean.TRUE.equals(sd.getCancelled())
                                    && Boolean.TRUE.equals(sd.getUsed()))
                            .findFirst()
                            .orElse(null);
                    if (usedService != null && newQuantity != usedService.getQuantity()) {
                        throw new AppException(ErrorCode.SERVICE_ALREADY_USED);
                    }
                    if (newQuantity > 0) {
                        log.warn("Service ID: {} not found in BookingDetail ID: {}.", sItem.getServiceId(), targetDetail.getId());
                    }
                    continue;
                }

                int oldQuantity = targetService.getQuantity();
                double pricePerUnit = targetService.getPrice() != null ? targetService.getPrice() : 0.0;

                if (newQuantity <= 0) {
                    // TRƯỜNG HỢP 1: Hủy dịch vụ (giảm về 0)
                    targetService.setCancelled(true);
                    targetService.setCancelledAt(LocalDateTime.now());

                    double deductedAmount = pricePerUnit * oldQuantity;
                    servicePriceChange = servicePriceChange.subtract(BigDecimal.valueOf(deductedAmount));

                    log.info("Cancelled service ID: {} in BookingDetail ID: {}. Reduced amount: {}",
                            sItem.getServiceId(), targetDetail.getId(), deductedAmount);

                } else if (newQuantity != oldQuantity) {
                    // TRƯỜNG HỢP 2: Thay đổi số lượng
                    int quantityDiff = newQuantity - oldQuantity;
                    double priceChangeAmount = pricePerUnit * quantityDiff;

                    targetService.setQuantity(newQuantity);
                    servicePriceChange = servicePriceChange.add(BigDecimal.valueOf(priceChangeAmount));

                    log.info("Updated service ID: {} in BookingDetail ID: {} from quantity {} to {}. Price change: {}",
                            sItem.getServiceId(), targetDetail.getId(), oldQuantity, newQuantity, priceChangeAmount);
                }
            }

            // =========================================================================
            // SAU KHI CẬP NHẬT CÁC DỊCH VỤ TRONG PHÒNG, TÍNH LẠI TOÀN BỘ TIỀN CHO PHÒNG NÀY
            // =========================================================================
            double roomSub = targetDetail.getRoomSubTotal() != null ? targetDetail.getRoomSubTotal() : 0.0;

            // Tính tổng tiền dịch vụ thực tế còn lại của phòng (chỉ tính các dịch vụ không bị hủy)
            double updatedServiceSubTotal = targetDetail.getBookingServiceDetails().stream()
                    .filter(sd -> !Boolean.TRUE.equals(sd.getCancelled()))
                    .mapToDouble(sd -> (sd.getPrice() != null ? sd.getPrice() : 0.0) * sd.getQuantity())
                    .sum();

            // Tổng tiền mới của phòng = Tiền phòng + Tổng tiền dịch vụ mới
            targetDetail.setTotalPrice(roomSub + updatedServiceSubTotal);
        }

        return servicePriceChange;
    }

    @Override
    public PromotionDiscountResult applyOrReevaluatePromotion(BookingModificationRequest request,
                                                              Order order,
                                                              BigDecimal totalRoom,
                                                              BigDecimal totalService) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal currentTotalAmountBeforeDiscount = totalRoom.add(totalService);
        Promotion promotionToUse = null;
        Booking booking = order.getBooking();

        if (Boolean.TRUE.equals(request.getRemovePromotion())) {
            promotionUsageService.replaceBookingPromotion(booking, null, null);
            booking.setApplyAt(null);
            return PromotionDiscountResult.builder()
                    .discountAmountTotal(BigDecimal.ZERO)
                    .discountRoomAmount(BigDecimal.ZERO)
                    .discountServiceAmount(BigDecimal.ZERO)
                    .build();
        }

        // =========================================================================
        // ƯU TIÊN 1: KIỂM XEM REQUEST CÓ TRUYỀN LÊN MÃ KHUYẾN MÃI MỚI HAY KHÔNG
        // =========================================================================

        // Trường hợp 1A: Có truyền PromotionRequest chung
        if (request.getPromotionRequest() != null && request.getPromotionRequest().getPromotionId() != null /* hoặc getCode() tùy DTO của bạn */) {
            String promoId = request.getPromotionRequest().getPromotionId();
            Promotion newPromotion = promotionRepository.findById(promoId)
                    .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

            Promotion currentPromotion = booking.getCustomerPromotion() != null
                    ? booking.getCustomerPromotion().getPromotion()
                    : booking.getPromotion();
            boolean samePromotion = currentPromotion != null
                    && currentPromotion.getId().equals(newPromotion.getId());
            if (samePromotion) {
                bookingServiceImpl.validatePromotionRulesPromotionOld(
                        newPromotion, currentTotalAmountBeforeDiscount, now, totalRoom, totalService);
            } else {
                bookingServiceImpl.validatePromotionRules(
                        newPromotion, currentTotalAmountBeforeDiscount, now, totalRoom, totalService);
            }

            promotionUsageService.replaceBookingPromotion(booking, newPromotion, null);
            if (!samePromotion) {
                booking.setApplyAt(now);
            }
            promotionToUse = newPromotion;
            log.info("Applied NEW general Promotion ID: {} during modification.", newPromotion.getId());

        }
        // Trường hợp 1B: Có truyền CustomerPromotionRequest riêng của khách
        else if (request.getCustomerPromotionRequest() != null && request.getCustomerPromotionRequest().getCustomerPromotionId() != null) {
            String customerPromoId = request.getCustomerPromotionRequest().getCustomerPromotionId();
            CustomerPromotion newCustomerPromotion = customerPromotionRepository.findById(customerPromoId)
                    .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
            if (booking.getCustomer() == null
                    || !booking.getCustomer().getId().equals(newCustomerPromotion.getCustomer().getId())) {
                throw new AppException(ErrorCode.UNAUTHORIZED_PROMOTION);
            }
            newCustomerPromotion = customerPromotionRepository.findByIdAndCustomerIdForUpdate(
                            customerPromoId,
                            booking.getCustomer().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED_PROMOTION));
            boolean voucherHeldByThisBooking = newCustomerPromotion.getBooking() != null
                    && newCustomerPromotion.getBooking().getId().equals(booking.getId());
            if ((newCustomerPromotion.isUsed() && !voucherHeldByThisBooking)
                    || (newCustomerPromotion.getBooking() != null && !voucherHeldByThisBooking)) {
                throw new AppException(ErrorCode.PROMOTION_ALREADY_USED);
            }
            Promotion newPromotion = newCustomerPromotion.getPromotion();

            Promotion currentPromotion = booking.getCustomerPromotion() != null
                    ? booking.getCustomerPromotion().getPromotion()
                    : booking.getPromotion();
            boolean samePromotion = currentPromotion != null
                    && currentPromotion.getId().equals(newPromotion.getId());
            if (samePromotion) {
                bookingServiceImpl.validatePromotionRulesPromotionOld(
                        newPromotion, currentTotalAmountBeforeDiscount, now, totalRoom, totalService);
            } else {
                bookingServiceImpl.validatePromotionRules(
                        newPromotion, currentTotalAmountBeforeDiscount, now, totalRoom, totalService);
            }

            promotionUsageService.replaceBookingPromotion(booking, newPromotion, newCustomerPromotion);
            if (!samePromotion) {
                booking.setApplyAt(now);
            }
            promotionToUse = newPromotion;
            log.info("Applied NEW customer-specific Promotion ID: {} during modification.", newPromotion.getId());
        }

        // =========================================================================
        // ƯU TIÊN 2: NẾU KHÔNG TRUYỀN MÃ MỚI -> DÙNG LẠI MÃ CŨ ĐANG CÓ TRONG ORDER
        // =========================================================================
        else {
            if (booking.getPromotion() == null && booking.getCustomerPromotion() == null) {
                // Không có mã nào từ trước và không truyền mới -> Không giảm giá
                return PromotionDiscountResult.builder()
                        .discountAmountTotal(BigDecimal.ZERO)
                        .discountRoomAmount(BigDecimal.ZERO)
                        .discountServiceAmount(BigDecimal.ZERO)
                        .build();
            }

            promotionToUse = booking.getCustomerPromotion() != null
                    ? booking.getCustomerPromotion().getPromotion()
                    : booking.getPromotion();
            // cho nay la check cái phần ngoại trừ cái ngày
            //bookingServiceImpl.validatePromotionRulesPromotionOld(promotionToUse, currentTotalAmountBeforeDiscount, now, totalRoom, totalService);

            try {
                // Validate lại mã cũ với số tiền mới sau khi chỉnh sửa
                // Dùng hàm validate chuẩn để nếu không thỏa mãn (min value, expired,...) sẽ nhảy vào catch
                bookingServiceImpl.validatePromotionRulesPromotionOld(promotionToUse, currentTotalAmountBeforeDiscount, now, totalRoom, totalService);
                log.info("Re-evaluating EXISTING Promotion ID: {} during modification.", promotionToUse.getId());

            } catch (AppException e) {
                // KHÔNG THỎA MÃN ĐIỀU KIỆN NỮA -> TỰ ĐỘNG GỠ BỎ MÃ KHỎI BOOKING
                log.warn("Promotion ID: {} no longer meets conditions after modification. Automatically removing promotion. Reason: {}",
                        promotionToUse.getId(), e.getErrorCode());

                promotionUsageService.replaceBookingPromotion(booking, null, null);
                booking.setApplyAt(null); // Gỡ luôn thời gian áp dụng nếu cần

                // Trả về kết quả giảm giá bằng 0 vì mã đã bị loại bỏ
                return PromotionDiscountResult.builder()
                        .discountAmountTotal(BigDecimal.ZERO)
                        .discountRoomAmount(BigDecimal.ZERO)
                        .discountServiceAmount(BigDecimal.ZERO)
                        .build();
            }
            promotionUsageService.replaceBookingPromotion(
                    booking,
                    promotionToUse,
                    booking.getCustomerPromotion()
            );
            log.info("Re-evaluating EXISTING Promotion ID: {} during modification.", promotionToUse.getId());
        }

        // =========================================================================
        // BƯỚC 3: TÍNH TOÁN LẠI CHI TIẾT MỨC GIẢM DỰA TRÊN PROMOTION ĐANG SỬ DỤNG
        // =========================================================================
        PromotionDiscountResult discountResult = bookingServiceImpl.calculateDiscountBreakdown(promotionToUse, totalRoom, totalService);

        log.info("Promotion applied successfully. Total discount: {}, Room discount: {}, Service discount: {}",
                discountResult.getDiscountAmountTotal(), discountResult.getDiscountRoomAmount(), discountResult.getDiscountServiceAmount());

        return discountResult;
    }

    private List<BookingNightlyPrice> calculateNightlyPrices(
            Long hotelId,
            Room room,
            LocalDate startDate,
            LocalDate endDate,
            Integer numAdults,
            Integer numChildren,
            BranchRoomPolicy policy) {
        ExtraFeeBreakdownResponse fees = roomPricingCalculator.calculateExtraFeeBreakdown(policy, numAdults, numChildren);
        BigDecimal adultFee = BigDecimal.valueOf(fees.getAdultExtraFee());
        BigDecimal childFee = BigDecimal.valueOf(fees.getChildExtraFee());
        double amenitiesPrice = room.getTotalAmenitiesPrice() != null ? room.getTotalAmenitiesPrice() : 0.0;
        List<BookingNightlyPrice> prices = new ArrayList<>();

        for (LocalDate stayDate = startDate; stayDate.isBefore(endDate); stayDate = stayDate.plusDays(1)) {
            Optional<RoomSeasonalRate> seasonalRate = roomSeasonalRateRepository.findActiveRateByDate(
                    hotelId, room.getRoomType(), stayDate);
            double basePrice = (seasonalRate.map(RoomSeasonalRate::getPrice)
                    .orElse(policy != null && policy.getBasePrice() != null ? policy.getBasePrice() : 0.0))
                    + amenitiesPrice;
            BigDecimal roomBasePrice = BigDecimal.valueOf(basePrice);

            BookingNightlyPrice nightlyPrice = new BookingNightlyPrice();
            nightlyPrice.setStayDate(stayDate);
            nightlyPrice.setRoomBasePrice(roomBasePrice);
            nightlyPrice.setExtraAdultFee(adultFee);
            nightlyPrice.setExtraChildFee(childFee);
            nightlyPrice.setExtraAdultUnitPrice(policy != null && policy.getExtraAdultFee() != null
                    ? BigDecimal.valueOf(policy.getExtraAdultFee()) : BigDecimal.ZERO);
            nightlyPrice.setExtraChildUnitPrice(policy != null && policy.getExtraChildFee() != null
                    ? BigDecimal.valueOf(policy.getExtraChildFee()) : BigDecimal.ZERO);
            nightlyPrice.setStandardCapacity(policy != null ? policy.getStandardCapacity() : null);
            nightlyPrice.setMaxExtraGuests(policy != null ? policy.getMaxExtraGuests() : null);
            nightlyPrice.setNightTotal(roomBasePrice.add(adultFee).add(childFee).setScale(2, RoundingMode.HALF_UP));
            prices.add(nightlyPrice);
        }
        return prices;
    }

    private ExtraFeeBreakdownResponse calculateSnapshotExtraFees(
            Integer adults,
            Integer children,
            BookingNightlyPrice snapshot) {
        int actualAdults = adults != null ? adults : 0;
        int actualChildren = children != null ? children : 0;
        int standardCapacity = snapshot.getStandardCapacity();
        int maxExtraGuests = snapshot.getMaxExtraGuests();
        int totalGuests = actualAdults + actualChildren;

        if (totalGuests > standardCapacity + maxExtraGuests) {
            throw new AppException(ErrorCode.EXCEEDS_MAX_CAPACITY);
        }
        if (totalGuests <= standardCapacity) {
            return new ExtraFeeBreakdownResponse(0.0, 0.0, 0.0);
        }

        int extraGuests = totalGuests - standardCapacity;
        if (extraGuests > maxExtraGuests) {
            throw new AppException(ErrorCode.EXCEEDS_MAX_EXTRA_GUESTS);
        }

        int adultsCoveredByStandard = Math.min(actualAdults, standardCapacity);
        int remainingStandardSlots = standardCapacity - adultsCoveredByStandard;
        int extraAdults = Math.max(0, actualAdults - adultsCoveredByStandard);
        int extraChildren = Math.max(0, actualChildren - remainingStandardSlots);
        double adultFee = extraAdults * snapshot.getExtraAdultUnitPrice().doubleValue();
        double childFee = extraChildren * snapshot.getExtraChildUnitPrice().doubleValue();
        return new ExtraFeeBreakdownResponse(adultFee, childFee, adultFee + childFee);
    }

    private BigDecimal totalNightlyPrice(List<BookingNightlyPrice> nightlyPrices) {
        return nightlyPrices.stream()
                .map(BookingNightlyPrice::getNightTotal)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void ensureLegacyNightlyPrices(BookingDetail detail) {
        if (detail.getNightlyPrices() == null) detail.setNightlyPrices(new ArrayList<>());
        if (!detail.getNightlyPrices().isEmpty()) return;

        LocalDate startDate = detail.getCheckinTime() != null ? detail.getCheckinTime().toLocalDate() : null;
        LocalDate endDate = detail.getCheckoutTime() != null ? detail.getCheckoutTime().toLocalDate() : null;
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new AppException(ErrorCode.INVALID_CHECKOUT_DATE);
        }

        long nightCount = ChronoUnit.DAYS.between(startDate, endDate);
        BigDecimal originalTotal = BigDecimal.valueOf(detail.getRoomSubTotal() != null ? detail.getRoomSubTotal() : 0.0)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal allocatedNightPrice = originalTotal.divide(BigDecimal.valueOf(nightCount), 2, RoundingMode.DOWN);
        BigDecimal allocatedTotal = BigDecimal.ZERO;

        for (LocalDate stayDate = startDate; stayDate.isBefore(endDate); stayDate = stayDate.plusDays(1)) {
            BigDecimal nightTotal = stayDate.plusDays(1).equals(endDate)
                    ? originalTotal.subtract(allocatedTotal)
                    : allocatedNightPrice;
            BookingNightlyPrice nightlyPrice = new BookingNightlyPrice();
            nightlyPrice.setStayDate(stayDate);
            nightlyPrice.setNightTotal(nightTotal);
            detail.addNightlyPrice(nightlyPrice);
            allocatedTotal = allocatedTotal.add(nightTotal);
        }
    }
}