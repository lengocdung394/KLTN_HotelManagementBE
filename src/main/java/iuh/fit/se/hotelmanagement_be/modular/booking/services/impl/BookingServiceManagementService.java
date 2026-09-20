package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingServiceDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingManagementRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.*;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.ExtraFeeBreakdownResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingManagementService;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomSeasonalRate;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomSeasonalRateRepository;
import iuh.fit.se.hotelmanagement_be.modular.service.repositories.ServiceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingServiceManagementService implements BookingManagementService {
    ServiceRepository serviceRepository;
    RoomRepository roomRepository;
    BookingManagementRepository bookingManagementRepository;
    EmployeeRepository employeeRepository;
    BranchRoomPolicyRepository branchRoomPolicyRepository;
    RoomSeasonalRateRepository roomSeasonalRateRepository;
    RoomPricingCalculator roomPricingCalculator;

    // Thêm phòng + kèm theo dịch vụ
    @Override
    public BigDecimal processRoomAdditions(Booking booking, List<NewRoomRequest> roomsToAdd) {
        if (roomsToAdd == null || roomsToAdd.isEmpty()) {
            log.debug("No rooms to add for booking ID: {}", booking.getId());
            return BigDecimal.ZERO;
        }

        log.info("Processing room additions. Total rooms to add: {} for booking ID: {}", roomsToAdd.size(), booking.getId());
        BigDecimal totalRoomAndServiceAdded = BigDecimal.ZERO;
        Long hotelId = booking.getOrder().getBooking().getEmployee().getHotel().getId();

        for (NewRoomRequest roomReq : roomsToAdd) {
            log.info("Adding room ID: {} from {} to {}, Adults: {}, Children: {}",
                    roomReq.getRoomId(), roomReq.getCheckInTime(), roomReq.getCheckOutTime(),
                    roomReq.getNumAdults(), roomReq.getNumChildren());

            Room room = roomRepository.findById(roomReq.getRoomId())
                    .orElseThrow(() -> {
                        log.error("Room not found with ID: {}", roomReq.getRoomId());
                        return new AppException(ErrorCode.ROOM_NOT_FOUND);
                    });

            BranchRoomPolicy policy = branchRoomPolicyRepository.findByHotelIdAndRoomType(hotelId, room.getRoomType());
            ExtraFeeBreakdownResponse extraFeeBreakdown = roomPricingCalculator.calculateExtraFeeBreakdown(policy, roomReq.getNumAdults(), roomReq.getNumChildren());
            double totalExtraFeePerNight = extraFeeBreakdown.getTotalExtraFee();

            LocalDate startDate = roomReq.getCheckInTime().toLocalDate();
            LocalDate endDate = roomReq.getCheckOutTime().toLocalDate();

            BigDecimal roomSubTotal = BigDecimal.ZERO;
            double sampleBasePricePerNight = 0.0;

            LocalDate currentDate = startDate;
            while (currentDate.isBefore(endDate)) {
                Optional<RoomSeasonalRate> seasonalRateOpt = roomSeasonalRateRepository.findActiveRateByDate(hotelId, room.getRoomType(), currentDate);

                double dailyRoomPrice;
                double amenitiesPrice = room.getTotalAmenitiesPrice() != null ? room.getTotalAmenitiesPrice() : 0.0;

                if (seasonalRateOpt.isPresent()) {
                    dailyRoomPrice = seasonalRateOpt.get().getPrice() + amenitiesPrice;
                    log.debug("Date {} applies seasonal rate price: {}", currentDate, dailyRoomPrice);
                } else {
                    double basePrice = policy.getBasePrice() != null ? policy.getBasePrice() : 0.0;
                    dailyRoomPrice = basePrice + amenitiesPrice;
                    log.debug("Date {} applies standard base price: {}", currentDate, dailyRoomPrice);
                }

                sampleBasePricePerNight = dailyRoomPrice;
                BigDecimal dailyTotal = BigDecimal.valueOf(dailyRoomPrice + totalExtraFeePerNight);
                roomSubTotal = roomSubTotal.add(dailyTotal);

                currentDate = currentDate.plusDays(1);
            }

            // 1. Khởi tạo BookingDetail mới
            BookingDetail newDetail = BookingDetail.builder()
                    .room(room)
                    .checkinTime(roomReq.getCheckInTime())
                    .checkoutTime(roomReq.getCheckOutTime())
                    .numAdults(roomReq.getNumAdults())
                    .numChildren(roomReq.getNumChildren())
                    .baseRoomPricePerNight(sampleBasePricePerNight)
                    .roomSubTotal(roomSubTotal.doubleValue())
                    .totalPrice(roomSubTotal.doubleValue())
                    .status(BookingStatusType.PENDING)
                    .build();

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

    // Đổi ngày checkin checkout
    @Override
    public BigDecimal processRoomDateUpdates(Booking booking, List<RoomDateUpdateRequest> dateUpdates) {
        if (dateUpdates == null || dateUpdates.isEmpty()) {
            log.debug("No room date updates for booking ID: {}", booking.getId());
            return BigDecimal.ZERO;
        }

        log.info("Processing room date updates. Total items: {} for booking ID: {}", dateUpdates.size(), booking.getId());
        BigDecimal roomPriceChange = BigDecimal.ZERO;
        Long hotelId = booking.getOrder().getBooking().getEmployee().getHotel().getId();

        for (RoomDateUpdateRequest updateReq : dateUpdates) {
            log.info("Updating dates for BookingDetail ID: {} to new Check-in: {}, Check-out: {}",
                    updateReq.getBookingDetailId(), updateReq.getNewCheckInTime(), updateReq.getNewCheckoutTime());

            BookingDetail detail = booking.getBookingDetails().stream()
                    .filter(d -> d.getId().equals(updateReq.getBookingDetailId()))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error("BookingDetail not found with ID: {}", updateReq.getBookingDetailId());
                        return new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND);
                    });

            if (detail.getStatus() == BookingStatusType.CANCELLED) {
                log.warn("Attempted to update dates for a cancelled BookingDetail ID: {}", detail.getId());
                throw new AppException(ErrorCode.CANNOT_UPDATE_DATE_FOR_CANCELLED_ROOM);
            }

            double oldSubTotal = detail.getRoomSubTotal() != null ? detail.getRoomSubTotal() : 0.0;
            roomPriceChange = roomPriceChange.subtract(BigDecimal.valueOf(oldSubTotal));
            log.debug("Deducted old room subtotal: {} for BookingDetail ID: {}", oldSubTotal, detail.getId());

            Room room = detail.getRoom();
            BranchRoomPolicy policy = branchRoomPolicyRepository.findByHotelIdAndRoomType(hotelId, room.getRoomType());
            ExtraFeeBreakdownResponse extraFeeBreakdown = roomPricingCalculator.calculateExtraFeeBreakdown(policy, detail.getNumAdults(), detail.getNumChildren());
            double totalExtraFeePerNight = extraFeeBreakdown.getTotalExtraFee();

            LocalDate startDate = updateReq.getNewCheckInTime().toLocalDate();
            LocalDate endDate = updateReq.getNewCheckoutTime().toLocalDate();

            BigDecimal newRoomSubTotal = BigDecimal.ZERO;
            double sampleBasePricePerNight = 0.0;

            LocalDate currentDate = startDate;
            while (currentDate.isBefore(endDate)) {
                Optional<RoomSeasonalRate> seasonalRateOpt = roomSeasonalRateRepository.findActiveRateByDate(hotelId, room.getRoomType(), currentDate);

                double dailyRoomPrice;
                double amenitiesPrice = room.getTotalAmenitiesPrice() != null ? room.getTotalAmenitiesPrice() : 0.0;

                if (seasonalRateOpt.isPresent()) {
                    dailyRoomPrice = seasonalRateOpt.get().getPrice() + amenitiesPrice;
                } else {
                    double basePrice = policy.getBasePrice() != null ? policy.getBasePrice() : 0.0;
                    dailyRoomPrice = basePrice + amenitiesPrice;
                }

                sampleBasePricePerNight = dailyRoomPrice;
                BigDecimal dailyTotal = BigDecimal.valueOf(dailyRoomPrice + totalExtraFeePerNight);
                newRoomSubTotal = newRoomSubTotal.add(dailyTotal);

                currentDate = currentDate.plusDays(1);
            }

            detail.setCheckinTime(updateReq.getNewCheckInTime());
            detail.setCheckoutTime(updateReq.getNewCheckoutTime());
            detail.setBaseRoomPricePerNight(sampleBasePricePerNight);
            detail.setRoomSubTotal(newRoomSubTotal.doubleValue());

            double currentServiceTotal = detail.getBookingServiceDetails() == null ? 0.0 :
                    detail.getBookingServiceDetails().stream()
                            .filter(sd -> !Boolean.TRUE.equals(sd.getCancelled()))
                            .mapToDouble(sd -> sd.getPrice() * sd.getQuantity())
                            .sum();

            detail.setTotalPrice(newRoomSubTotal.add(BigDecimal.valueOf(currentServiceTotal)).doubleValue());
            roomPriceChange = roomPriceChange.add(newRoomSubTotal);
            log.info("Successfully updated dates for BookingDetail ID: {}. New room subtotal: {}", detail.getId(), newRoomSubTotal);
        }

        return roomPriceChange;
    }

    // Hàm Nhạc Trưởng
    @Override
    public BookingModificationRequest modifyBooking(Long bookingId, BookingModificationRequest request) {
        log.info("==> START modifying booking ID: {} by Employee ID: {}", bookingId, request.getEmployeeId());

        Booking booking = bookingManagementRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.error("Booking not found with ID: {}", bookingId);
                    return new AppException(ErrorCode.BOOKING_NOT_FOUND);
                });

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            log.warn("Attempted to modify an already cancelled booking ID: {}", bookingId);
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED);
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

        // 1. Xử lý hủy phòng hàng loạt
        roomPriceChange = roomPriceChange.add(processCancellations(booking, request.getBookingDetailIdsToCancel(), operatorName));

        // 2. Xử lý thêm phòng mới (kèm dịch vụ) hàng loạt
        roomPriceChange = roomPriceChange.add(processRoomAdditions(booking, request.getRoomsToAdd()));

        // 3. Xử lý đổi phòng hàng loạt
        roomPriceChange = roomPriceChange.add(processRoomChanges(booking, request.getRoomsToChange()));

        // 4. Xử lý cập nhật ngày checkin/checkout hàng loạt
        roomPriceChange = roomPriceChange.add(processRoomDateUpdates(booking, request.getRoomsToUpdateDates()));

        // 5. Xử lý hủy dịch vụ lẻ hàng loạt
        servicePriceChange = servicePriceChange.add(processServiceCancellations(booking, request.getServicesToCancel()));

        // 6. Cập nhật lại tổng tiền vào Order chung
        BigDecimal totalOrderChange = roomPriceChange.add(servicePriceChange);
        BigDecimal oldTotalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal newTotalAmount = oldTotalAmount.add(totalOrderChange);

        order.setTotalAmount(newTotalAmount);

        log.info("Booking modification summary for Booking ID: {} -> Room price change: {}, Service price change: {}, Old total: {}, New total: {}",
                bookingId, roomPriceChange, servicePriceChange, oldTotalAmount, newTotalAmount);

        bookingManagementRepository.save(booking);
        log.info("<== FINISHED modifying booking ID: {} successfully.", bookingId);

        return request;
    }

    // Đổi phòng
    @Override
    public BigDecimal processRoomChanges(Booking booking, List<UpdateRoomChangeRequest> roomChanges) {
        if (roomChanges == null || roomChanges.isEmpty()) {
            log.debug("No room changes requested for booking ID: {}", booking.getId());
            return BigDecimal.ZERO;
        }

        log.info("Processing room changes. Total requests: {} for booking ID: {}", roomChanges.size(), booking.getId());
        BigDecimal roomPriceChange = BigDecimal.ZERO;
        Long hotelId = booking.getOrder().getBooking().getEmployee().getHotel().getId();

        for (UpdateRoomChangeRequest changeReq : roomChanges) {
            log.info("Changing BookingDetail ID: {} to new Room ID: {}", changeReq.getBookingDetailId(), changeReq.getNewRoomId());

            BookingDetail oldDetail = booking.getBookingDetails().stream()
                    .filter(d -> d.getId().equals(changeReq.getBookingDetailId()))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error("Old BookingDetail not found with ID: {}", changeReq.getBookingDetailId());
                        return new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND);
                    });

            if (oldDetail.getStatus() == BookingStatusType.CANCELLED) {
                log.warn("Attempted to change a cancelled BookingDetail ID: {}", oldDetail.getId());
                throw new AppException(ErrorCode.CANNOT_CHANGE_CANCELLED_ROOM);
            }

            if (oldDetail.getRoomSubTotal() != null) {
                roomPriceChange = roomPriceChange.subtract(BigDecimal.valueOf(oldDetail.getRoomSubTotal()));
                log.debug("Deducted old room subtotal: {}", oldDetail.getRoomSubTotal());
            }

            oldDetail.setStatus(BookingStatusType.CANCELLED);
            oldDetail.setCancelledAt(LocalDateTime.now());

            Room newRoom = roomRepository.findById(changeReq.getNewRoomId())
                    .orElseThrow(() -> {
                        log.error("New Room not found with ID: {}", changeReq.getNewRoomId());
                        return new AppException(ErrorCode.ROOM_NOT_FOUND);
                    });

            BranchRoomPolicy policy = branchRoomPolicyRepository.findByHotelIdAndRoomType(hotelId, newRoom.getRoomType());
            ExtraFeeBreakdownResponse extraFeeBreakdown = roomPricingCalculator.calculateExtraFeeBreakdown(policy, oldDetail.getNumAdults(), oldDetail.getNumChildren());
            double totalExtraFeePerNight = extraFeeBreakdown.getTotalExtraFee();

            LocalDate startDate = oldDetail.getCheckinTime().toLocalDate();
            LocalDate endDate = oldDetail.getCheckoutTime().toLocalDate();

            BigDecimal newRoomSubTotal = BigDecimal.ZERO;
            double sampleBasePricePerNight = 0.0;

            LocalDate currentDate = startDate;
            while (currentDate.isBefore(endDate)) {
                Optional<RoomSeasonalRate> seasonalRateOpt = roomSeasonalRateRepository.findActiveRateByDate(hotelId, newRoom.getRoomType(), currentDate);

                double dailyRoomPrice;
                double amenitiesPrice = newRoom.getTotalAmenitiesPrice() != null ? newRoom.getTotalAmenitiesPrice() : 0.0;

                if (seasonalRateOpt.isPresent()) {
                    dailyRoomPrice = seasonalRateOpt.get().getPrice() + amenitiesPrice;
                } else {
                    double basePrice = policy.getBasePrice() != null ? policy.getBasePrice() : 0.0;
                    dailyRoomPrice = basePrice + amenitiesPrice;
                }

                sampleBasePricePerNight = dailyRoomPrice;
                BigDecimal dailyTotal = BigDecimal.valueOf(dailyRoomPrice + totalExtraFeePerNight);
                newRoomSubTotal = newRoomSubTotal.add(dailyTotal);

                currentDate = currentDate.plusDays(1);
            }

            BookingDetail newDetail = BookingDetail.builder()
                    .room(newRoom)
                    .checkinTime(oldDetail.getCheckinTime())
                    .checkoutTime(oldDetail.getCheckoutTime())
                    .numAdults(oldDetail.getNumAdults())
                    .numChildren(oldDetail.getNumChildren())
                    .baseRoomPricePerNight(sampleBasePricePerNight)
                    .roomSubTotal(newRoomSubTotal.doubleValue())
                    .totalPrice(newRoomSubTotal.doubleValue())
                    .status(BookingStatusType.PENDING)
                    .build();

            booking.addBookingDetail(newDetail);
            roomPriceChange = roomPriceChange.add(newRoomSubTotal);
            log.info("Successfully changed room. New room subtotal added: {}", newRoomSubTotal);
        }

        return roomPriceChange;
    }

    // Hủy nhiều dịch vụ cho nhiều phòng
    @Override
    public BigDecimal processServiceCancellations(Booking booking, List<ServiceCancellationRequest> cancellations) {
        if (cancellations == null || cancellations.isEmpty()) {
            log.debug("No service cancellations requested for booking ID: {}", booking.getId());
            return BigDecimal.ZERO;
        }

        log.info("Processing service cancellations. Total items: {} for booking ID: {}", cancellations.size(), booking.getId());
        BigDecimal servicePriceReduced = BigDecimal.ZERO;

        for (ServiceCancellationRequest cancelReq : cancellations) {
            BookingDetail targetDetail = booking.getBookingDetails().stream()
                    .filter(d -> d.getId().equals(cancelReq.getBookingDetailId()))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error("BookingDetail not found for service cancellation with ID: {}", cancelReq.getBookingDetailId());
                        return new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND);
                    });

            if (targetDetail.getStatus() == BookingStatusType.CANCELLED) {
                log.warn("Attempted to cancel services in a cancelled BookingDetail ID: {}", targetDetail.getId());
                throw new AppException(ErrorCode.CANNOT_CANCEL_SERVICE_IN_CANCELLED_ROOM);
            }

            if (cancelReq.getServiceDetailIds() == null || cancelReq.getServiceDetailIds().isEmpty()) {
                continue;
            }

            for (Long serviceDetailId : cancelReq.getServiceDetailIds()) {
                BookingServiceDetail targetService = targetDetail.getBookingServiceDetails().stream()
                        .filter(sd -> sd.getId().equals(serviceDetailId))
                        .findFirst()
                        .orElseThrow(() -> {
                            log.error("BookingServiceDetail not found with ID: {}", serviceDetailId);
                            return new AppException(ErrorCode.BOOKING_SERVICE_DETAIL_NOT_FOUND);
                        });

                if (Boolean.TRUE.equals(targetService.getCancelled())) {
                    log.debug("Service detail ID: {} is already cancelled. Skipping.", serviceDetailId);
                    continue;
                }

                targetService.setCancelled(true);
                targetService.setCancelledAt(LocalDateTime.now());

                if (targetService.getPrice() != null) {
                    BigDecimal price = BigDecimal.valueOf(targetService.getPrice());
                    BigDecimal quantity = BigDecimal.valueOf(targetService.getQuantity());
                    BigDecimal itemTotal = price.multiply(quantity);

                    servicePriceReduced = servicePriceReduced.subtract(itemTotal);
                    log.info("Cancelled service ID: {}, Amount reduced: {}", serviceDetailId, itemTotal);
                }
            }
        }

        return servicePriceReduced;
    }

    // Hủy nhiều phòng (đã kèm dịch vụ)
    @Override
    public BigDecimal processCancellations(Booking booking, List<Long> detailIdsToCancel, String operatorName) {
        if (detailIdsToCancel == null || detailIdsToCancel.isEmpty()) {
            log.debug("No room cancellations requested for booking ID: {}", booking.getId());
            return BigDecimal.ZERO;
        }

        log.info("Processing room cancellations. Total rooms to cancel: {} by operator: {}", detailIdsToCancel.size(), operatorName);
        BigDecimal roomPriceReduced = BigDecimal.ZERO;

        for (Long detailId : detailIdsToCancel) {
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
}