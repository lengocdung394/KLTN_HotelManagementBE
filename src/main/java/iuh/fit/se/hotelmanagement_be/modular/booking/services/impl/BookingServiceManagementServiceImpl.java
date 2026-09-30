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
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingModificationResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.ExtraFeeBreakdownResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.RoomPriceCalculationResult;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingManagementService;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentStatus;
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
public class BookingServiceManagementServiceImpl implements BookingManagementService {
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

            // Trừ đi số tiền phòng cũ để tính chênh lệch về sau
            double oldRoomSub = targetDetail.getRoomSubTotal() != null ? targetDetail.getRoomSubTotal() : 0.0;
            totalRoomPriceChange = totalRoomPriceChange.subtract(BigDecimal.valueOf(oldRoomSub));

            // 2. Cập nhật phòng mới (nếu có truyền `newRoomId`)
            Room roomToUse = targetDetail.getRoom();
            if (req.getNewRoomId() != null && !req.getNewRoomId().equals(roomToUse.getId())) {
                Room newRoom = roomRepository.findById(req.getNewRoomId())
                        .orElseThrow(() -> {
                            log.error("New Room not found with ID: {}", req.getNewRoomId());
                            return new AppException(ErrorCode.ROOM_NOT_FOUND);
                        });
                targetDetail.setRoom(newRoom);
                roomToUse = newRoom;
                log.info("Updated room ID to {} for BookingDetail ID: {}", newRoom.getId(), targetDetail.getId());
            }

            // 3. Cập nhật ngày tháng (nếu có truyền lên)
            LocalDateTime checkInToUse = req.getNewCheckInTime() != null ? req.getNewCheckInTime() : targetDetail.getCheckinTime();
            LocalDateTime checkOutToUse = req.getNewCheckoutTime() != null ? req.getNewCheckoutTime() : targetDetail.getCheckoutTime();

            targetDetail.setCheckinTime(checkInToUse);
            targetDetail.setCheckoutTime(checkOutToUse);

            // 4. Cập nhật số lượng người (nếu có truyền lên)
            if (req.getNumAdults() != null) {
                targetDetail.setNumAdults(req.getNumAdults());
            }
            if (req.getNumChildren() != null) {
                targetDetail.setNumChildren(req.getNumChildren());
            }
            if (req.getNumInfants() != null) {
                targetDetail.setNumInfants(req.getNumInfants());
            }

            // 5. Tính toán lại toàn bộ tiền phòng và phụ thu qua hàm chuẩn
            BranchRoomPolicy policy = branchRoomPolicyRepository.findByHotelIdAndRoomType(hotelId, roomToUse.getRoomType());

            RoomPriceCalculationResult calcResult = calculateRoomTotalAndFees(
                    hotelId, roomToUse, checkInToUse, checkOutToUse,
                    targetDetail.getNumAdults(), targetDetail.getNumChildren(), policy
            );

            // Gán kết quả tính toán mới
            targetDetail.setBaseRoomPricePerNight(calcResult.getBasePricePerNight());
            targetDetail.setExtraAdultFeePerNight(calcResult.getTotalExtraFeePerNight());

            double newRoomSubDouble = calcResult.getRoomSubTotal().doubleValue();
            targetDetail.setRoomSubTotal(newRoomSubDouble);

            // Tính lại tổng tiền riêng của BookingDetail (Tiền phòng mới + Tiền dịch vụ không bị hủy)
            double currentServiceSub = targetDetail.getBookingServiceDetails() == null ? 0.0 :
                    targetDetail.getBookingServiceDetails().stream()
                            .filter(sd -> !Boolean.TRUE.equals(sd.getCancelled()))
                            .mapToDouble(sd -> (sd.getPrice() != null ? sd.getPrice() : 0.0) * sd.getQuantity())
                            .sum();

            targetDetail.setTotalPrice(newRoomSubDouble + currentServiceSub);

            // Cộng dồn tiền phòng mới vào biến chênh lệch tổng đơn hàng
            totalRoomPriceChange = totalRoomPriceChange.add(BigDecimal.valueOf(newRoomSubDouble));
            log.info("Successfully updated BookingDetail ID: {}. New Room Subtotal: {}", targetDetail.getId(), newRoomSubDouble);
        }

        return totalRoomPriceChange;
    }
    // Hàm Nhạc Trưởng
    @Override
    public BookingModificationResponse modifyBooking(String bookingId, BookingModificationRequest request) {
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

        // 1. Xử lý hủy phòng hàng loạt -  huy luon dich vu
        roomPriceChange = roomPriceChange.add(processCancellations(booking, request.getBookingDetailIdsToCancel(), operatorName));

        // 2. Xử lý thêm phòng mới (kèm dịch vụ) hàng loạt
        roomPriceChange = roomPriceChange.add(processRoomAdditions(booking, request.getRoomsToAdd()));

        // 4. Xử lý cập nhật ngày checkin/checkout hàng loạt co the la doi phong kem theo so luong nguoi
        roomPriceChange = roomPriceChange.add(processRoomUpdates(booking, request.getRoomsToChange()));

        // 4. Xử lý thêm dịch vụ phát sinh cho phòng cũ
        servicePriceChange = servicePriceChange.add(processServicesForExistingRooms(booking, request.getServicesToAddForExistingRooms()));

        // 5. Xử lý cập nhật/giảm số lượng dịch vụ theo phòng -- TH huy luon nhieu dich vu cho phong do
        servicePriceChange = servicePriceChange.add(processServiceQuantityUpdates(booking, request.getServiceQuantityUpdates()));

        // ==========================================
        // 7. TÍNH TOÁN & CẬP NHẬT CHUẨN XÁC TỪNG PHÒNG VÀ ORDER
        // ==========================================
        BigDecimal totalRoom = BigDecimal.ZERO;
        BigDecimal totalService = BigDecimal.ZERO;

        for (BookingDetail detail : booking.getBookingDetails()) {
            if (detail.getStatus() == BookingStatusType.CANCELLED) {
                detail.setTotalPrice(0.0);
                continue;
            }

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

        BigDecimal discount = order.getDiscountAmountTotal() != null ? order.getDiscountAmountTotal() : BigDecimal.ZERO;
        BigDecimal newTotalAmount = totalRoom.add(totalService).subtract(discount);
        BigDecimal oldTotalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal totalOrderChange = newTotalAmount.subtract(oldTotalAmount);


        // Giả sử trong entity Order của bạn có trường paidAmount lưu tổng tiền khách đã trả trước đó
        BigDecimal paidAmount = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;

        // Tính toán lại số tiền còn phải trả sau khi modify
        BigDecimal remainingAmount = newTotalAmount.subtract(paidAmount);

        // Gán lại các giá trị tổng cho Order
        order.setRoomTotalAmount(totalRoom);
        order.setServiceTotalAmount(totalService);
        order.setTotalAmount(newTotalAmount);
        order.setRemainingAmount(remainingAmount); // Cập nhật số tiền còn thiếu hoặc cần hoàn

        // ---> BỔ SUNG THÊM ĐOẠN CẬP NHẬT TRẠNG THÁI THANH TOÁN (PAYMENT STATUS) NÀY <---
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
        log.info("Booking modification summary for Booking ID: {} -> Total Room: {}, Total Service: {}, Old total: {}, New total: {}",
                bookingId, totalRoom, totalService, oldTotalAmount, newTotalAmount);

        bookingManagementRepository.save(booking);
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
                        .filter(sd -> sd.getService().getId().equals(sReq.getServiceId()) && !Boolean.TRUE.equals(sd.getCancelled()))
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
                    if (sReq.getUsedAt() != null) {
                        existingServiceDetail.setUsedAt(sReq.getUsedAt());
                    }

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
                            .usedAt(sReq.getUsedAt()) // Gán thời gian sử dụng từ request lên
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
    // Ham giam so luong dich vu giam so luong dich vu
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
                // 2. Tìm dịch vụ trong phòng đó dựa vào serviceId
                BookingServiceDetail targetService = targetDetail.getBookingServiceDetails().stream()
                        .filter(sd -> sd.getService().getId().equals(sItem.getServiceId()) && !Boolean.TRUE.equals(sd.getCancelled()))
                        .findFirst()
                        .orElse(null);

                int newQuantity = sItem.getQuantity();

                if (targetService == null) {
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

    // ham phu tro tinh toan tien phong kem theo so luong nguoi
    private RoomPriceCalculationResult calculateRoomTotalAndFees(
            Long hotelId, Room room, LocalDateTime checkIn, LocalDateTime checkOut, Integer numAdults, Integer numChildren, BranchRoomPolicy policy) {

        // 1. Tính phụ thu qua RoomPricingCalculator đã có sẵn
        ExtraFeeBreakdownResponse extraFeeBreakdown = roomPricingCalculator.calculateExtraFeeBreakdown(policy, numAdults, numChildren);
        double totalExtraFeePerNight = extraFeeBreakdown.getTotalExtraFee();

        LocalDate startDate = checkIn.toLocalDate();
        LocalDate endDate = checkOut.toLocalDate();

        BigDecimal roomSubTotal = BigDecimal.ZERO;
        double sampleBasePricePerNight = 0.0;

        LocalDate currentDate = startDate;
        while (currentDate.isBefore(endDate)) {
            Optional<RoomSeasonalRate> seasonalRateOpt = roomSeasonalRateRepository.findActiveRateByDate(hotelId, room.getRoomType(), currentDate);

            double dailyRoomPrice;
            double amenitiesPrice = room.getTotalAmenitiesPrice() != null ? room.getTotalAmenitiesPrice() : 0.0;

            if (seasonalRateOpt.isPresent()) {
                dailyRoomPrice = seasonalRateOpt.get().getPrice() + amenitiesPrice;
            } else {
                double basePrice = policy != null && policy.getBasePrice() != null ? policy.getBasePrice() : 0.0;
                dailyRoomPrice = basePrice + amenitiesPrice;
            }

            sampleBasePricePerNight = dailyRoomPrice;
            BigDecimal dailyTotal = BigDecimal.valueOf(dailyRoomPrice + totalExtraFeePerNight);
            roomSubTotal = roomSubTotal.add(dailyTotal);

            currentDate = currentDate.plusDays(1);
        }

        return new RoomPriceCalculationResult(
                roomSubTotal,
                sampleBasePricePerNight,
                extraFeeBreakdown.getTotalExtraFee()
        );
    }
}