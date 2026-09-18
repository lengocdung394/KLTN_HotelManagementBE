package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.CustomerRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingServiceDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingDetailCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingServiceRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.*;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingService;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.CustomerPromotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.Promotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionStatus;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.CustomerPromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.PromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomSeasonalRate;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomSeasonalRateRepository;
import iuh.fit.se.hotelmanagement_be.modular.service.repositories.ServiceRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.rmi.server.LogStream.log;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingServiceImpl implements BookingService {

    CustomerRepository customerRepository;
    EmployeeRepository employeeRepository;
    RoomRepository roomRepository;
    BranchRoomPolicyRepository branchRoomPolicyRepository;
    RoomPricingCalculator roomPricingCalculator;
    ServiceRepository serviceRepository;
    BookingRepository bookingRepository;
    CustomerPromotionRepository customerPromotionRepository;
    PromotionRepository promotionRepository;
    private final RoomSeasonalRateRepository roomSeasonalRateRepository;

    /**
     * LUỒNG CHÍNH 1: Khách hàng đặt online
     */
    @Transactional
    @Override
    public BookingResponse createCustomerBooking(BookingCreateRequest request) {
        Customer customer = validateAndGetCustomer(request.getCustomerId());
        Booking booking = initBookingForCustomer(customer, BookingChannel.ONLINE, BookingStatus.PENDING);

        List<BookingDetail> details = processBookingDetails(booking, request.getBookingDetails());
        booking.setBookingDetails(details);

        Order order = calculateAndBuildOrder(customer.getId(), request, details, booking);
        booking.setOrder(order);

        Booking savedBooking = bookingRepository.save(booking);
        return toBookingResponse(savedBooking);
    }

    /**
     * LUỒNG CHÍNH 2: Nhân viên đặt tại quầy
     */
    @Transactional
    @Override
    public BookingResponse createCounterBooking(Long employeeId, BookingCreateRequest request) {
        Customer customer = validateAndGetCustomer(request.getCustomerId());
        Employee employee = validateAndGetEmployee(employeeId);
        Booking booking = initBookingForEmployee(customer, employee, BookingChannel.OFFLINE, BookingStatus.CONFIRMED);

        List<BookingDetail> details = processBookingDetails(booking, request.getBookingDetails());
        booking.setBookingDetails(details);

        Order order = calculateAndBuildOrder(customer.getId(), request, details, booking);
        booking.setOrder(order);

        Booking savedBooking = bookingRepository.save(booking);
        return toBookingResponse(savedBooking);
    }

    /**
     * HELPER METHOD: Gom toàn bộ logic tính tiền phòng, dịch vụ, và áp dụng voucher
     */
    private Order calculateAndBuildOrder(Long customerId, BookingCreateRequest request, List<BookingDetail> details, Booking booking) {
        BigDecimal roomTotal = calculateTotalRoomPrice(details);
        BigDecimal serviceTotal = calculateTotalServicePrice(details);
        BigDecimal currentSubTotal = roomTotal.add(serviceTotal);
        BigDecimal discountTotal = BigDecimal.ZERO;

        // truong hop xu dung khuyen mai cua chi nhanh / truong hop xu dung khuyen mai cua rieng minh
        if (request.getPromotionId() != null) {
            Promotion p = promotionRepository.findById(request.getPromotionId())
                    .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
            discountTotal = applyPromotion(customerId, p.getCode(), currentSubTotal, roomTotal, serviceTotal, booking);
        } else if (request.getCustomerPromotionId() != null) {
            CustomerPromotion customerPromo = customerPromotionRepository.findById(request.getCustomerPromotionId())
                    .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

            if (!customerPromo.getCustomer().getId().equals(customerId)) {
                throw new AppException(ErrorCode.UNAUTHORIZED_PROMOTION);
            }
            discountTotal = applyPromotion(customerId, customerPromo.getUniqueCode(), currentSubTotal, roomTotal, serviceTotal, booking);
        }

        return createAndLinkOrder(roomTotal, serviceTotal, discountTotal);
    }

    private Customer validateAndGetCustomer(Long customerId) {
        return customerRepository.findById(customerId).orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
    }

    private Employee validateAndGetEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));
    }

    private Booking initBookingForCustomer(Customer customer, BookingChannel channel, BookingStatus status) {
        return Booking.builder()
                .customer(customer)
                .bookingChannel(channel)
                .bookingStatus(status)
                .build();
    }

    private Booking initBookingForEmployee(Customer customer, Employee employee, BookingChannel channel, BookingStatus status) {
        return Booking.builder()
                .customer(customer)
                .employee(employee)
                .bookingChannel(channel)
                .bookingStatus(status)
                .build();
    }

    @Override
    public BookingResponse toBookingResponse(Booking booking) {
        if (booking == null) {
            return null;
        }

        List<BookingDetailResponse> detailResponses = null;

        if (booking.getBookingDetails() != null) {
            detailResponses = booking.getBookingDetails().stream().map(detail ->
                    BookingDetailResponse.builder()
                            .bookingDetailId(detail.getId())
                            .roomId(detail.getRoom() != null ? detail.getRoom().getId() : null)
                            .roomName(detail.getRoom() != null ? detail.getRoom().getRoomType().toString() : null)
                            .roomTypeName(detail.getRoom() != null ? detail.getRoom().getRoomType().name() : null)
                            .checkInTime(detail.getCheckinTime())
                            .checkOutTime(detail.getCheckoutTime())
                            .numAdults(detail.getNumAdults())
                            .numChildren(detail.getNumChildren())
                            // Map thẳng các khoản chi tiết vào Response
                            .baseRoomPricePerNight(detail.getBaseRoomPricePerNight())
                            .extraAdultFeePerNight(detail.getExtraAdultFeePerNight())
                            .extraChildFeePerNight(detail.getExtraChildFeePerNight())
                            .roomSubTotal(detail.getRoomSubTotal())
                            .serviceSubTotal(detail.getServiceSubTotal())
                            .totalPrice(detail.getTotalPrice())
                            .build()
            ).toList();
        }
        Order order = booking.getOrder();
        return BookingResponse.builder()
                .orderId(order.getId())
                .bookingId(booking.getId())
                .customerId(booking.getCustomer() != null ? booking.getCustomer().getId() : null)
                .customerName(booking.getCustomer() != null ? booking.getCustomer().getFullName() : null)
                .bookingStatus(booking.getBookingStatus())
                .bookingChannel(booking.getBookingChannel())
                .createdAt(booking.getCreatedAt())
                .roomTotal(order != null ? order.getRoomTotalAmount() : null)
                .serviceTotal(order != null ? order.getServiceTotalAmount() : null)
                .discountTotal(order != null ? order.getDiscountAmountTotal() : null)
                .finalAmount(order != null ? order.getTotalAmount() : null)
                .bookingDetails(detailResponses)
                .build();
    }

    private Order createAndLinkOrder(BigDecimal roomTotal, BigDecimal serviceTotal, BigDecimal discountTotal) {
        return Order.builder()
                .issueDate(LocalDateTime.now())
                .roomTotalAmount(roomTotal)
                .serviceTotalAmount(serviceTotal)
                .discountAmountTotal(discountTotal)
                .paidAmount(BigDecimal.ZERO)
                .orderStatus(OrderStatusType.OPEN)
                .build();
    }

    @Override
    public List<BookingDetail> processBookingDetails(Booking booking, List<BookingDetailCreateRequest> detailRequests) {
        if (detailRequests == null || detailRequests.isEmpty()) {
            throw new AppException(ErrorCode.BOOKING_DETAILS_REQUIRED);
        }

        return detailRequests.stream().map(detailReq -> {
            Room room = roomRepository.findById(detailReq.getRoomId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

            Long hotelId = room.getFloor().getBuilding().getHotel().getId();
            BranchRoomPolicy policy = branchRoomPolicyRepository
                    .findByHotelIdAndRoomType(hotelId, room.getRoomType());

            if (policy == null) {
                throw new AppException(ErrorCode.BRANCH_POLICY_NOT_FOUND);
            }

            // 1. Tính tiền phụ thu (người lớn/trẻ em) cho 1 đêm từ Policy
            ExtraFeeBreakdownResponse extraFeeBreakdown = roomPricingCalculator.calculateExtraFeeBreakdown(
                    policy,
                    detailReq.getNumAdults(),
                    detailReq.getNumChildren()
            );

            double extraAdultFeePerNight = extraFeeBreakdown.getAdultExtraFee();
            double extraChildFeePerNight = extraFeeBreakdown.getChildExtraFee();
            double totalExtraFeePerNight = extraFeeBreakdown.getTotalExtraFee();

            // 2. Xác định ngày check-in, check-out và số đêm
            LocalDate checkInDate = detailReq.getCheckInTime().toLocalDate();
            LocalDate checkOutDate = detailReq.getCheckOutTime().toLocalDate();

            if (!checkOutDate.isAfter(checkInDate)) {
                throw new AppException(ErrorCode.INVALID_CHECKOUT_DATE);
            }

            long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            if (nights <= 0) nights = 1;

            // 3. Vòng lặp quét từng ngày (Daily Rate) để cộng dồn tiền phòng
            double totalRoomAmountForStay = 0.0;
            double accumulatedBasePrice = 0.0; // Dùng để tính giá gốc trung bình mỗi đêm
            LocalDate currentDate = checkInDate;

            while (currentDate.isBefore(checkOutDate)) {
                Optional<RoomSeasonalRate> seasonalRateOpt = roomSeasonalRateRepository
                        .findActiveRateByDate(hotelId, room.getRoomType(), currentDate);

                double dailyRoomPrice;
                double amenitiesPrice = room.getTotalAmenitiesPrice();
                if (seasonalRateOpt.isPresent()) {
                    dailyRoomPrice = seasonalRateOpt.get().getPrice() + amenitiesPrice;
                } else {
                    double basePrice = policy.getBasePrice() != null ? policy.getBasePrice() : 0.0;
                    dailyRoomPrice = basePrice + amenitiesPrice;
                }

                accumulatedBasePrice += dailyRoomPrice;
                // Tiền phòng ngày đó + phụ thu đêm đó
                totalRoomAmountForStay += (dailyRoomPrice + totalExtraFeePerNight);

                currentDate = currentDate.plusDays(1);
            }

            // Tính giá phòng gốc trung bình 1 đêm
            double baseRoomPricePerNight = accumulatedBasePrice / nights;
            double roomSubTotal = totalRoomAmountForStay; // Tổng tiền phòng + phụ thu toàn kỳ

            // 4. Xử lý dịch vụ đi kèm và tính tổng tiền dịch vụ
            List<BookingServiceDetail> serviceDetails = processAndAttachServices(null, detailReq.getServiceRequests());
            double serviceSubTotal = serviceDetails.stream()
                    .filter(sd -> !Boolean.TRUE.equals(sd.getIsPaid()))
                    .mapToDouble(sd -> sd.getPrice() * sd.getQuantity())
                    .sum(); //

            // cap nhat trang thai dich vu da duoc cong tien
            markServicesAsProcessedOrPaid(serviceDetails);
            // 5. Tổng cộng cuối cùng của phòng này (Phòng + Dịch vụ)
            double totalPrice = roomSubTotal + serviceSubTotal;

            // 6. Xây dựng Entity BookingDetail đầy đủ các khoản chi tiết
            BookingDetail bookingDetail = BookingDetail.builder()
                    .booking(booking)
                    .room(room)
                    .checkinTime(detailReq.getCheckInTime())
                    .checkoutTime(detailReq.getCheckOutTime())
                    .numAdults(detailReq.getNumAdults())
                    .numChildren(detailReq.getNumChildren())
                    .baseRoomPricePerNight(baseRoomPricePerNight)
                    .extraAdultFeePerNight(extraAdultFeePerNight)
                    .extraChildFeePerNight(extraChildFeePerNight)
                    .roomSubTotal(roomSubTotal)
                    .serviceSubTotal(serviceSubTotal)
                    .totalPrice(totalPrice)
                    .build();

            if (!serviceDetails.isEmpty()) {
                serviceDetails.forEach(sd -> sd.setBookingDetail(bookingDetail));
                bookingDetail.setBookingServiceDetails(serviceDetails);
            }

            booking.addBookingDetail(bookingDetail);
            return bookingDetail;
        }).toList();
    }

    // Hàm cập nhật những dịch vụ đã được tính tiền
    private void markServicesAsProcessedOrPaid(List<BookingServiceDetail> serviceDetails) {
        if (serviceDetails == null || serviceDetails.isEmpty()) {
            return;
        }
        for (BookingServiceDetail sd : serviceDetails) {
            // Chỉ cập nhật những món chưa thanh toán
            if (!Boolean.TRUE.equals(sd.getIsPaid())) {
                sd.setIsPaid(true);
            }
        }
        log("Đã cập nhật xong nhung dich vu da duoc tinh tien");
    }

    // Ham nay sử dụng khi checkout tính tiền phần dịch vụ chưa được tính tiền
    @Override
    public CheckoutSummaryResponse getCheckoutSummary(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).get();
        Order order = booking.getOrder();

        // Lọc ra tất cả các dịch vụ CHƯA thanh toán (isPaid = false)
        List<BookingServiceDetail> unpaidServices = booking.getBookingDetails().stream()
                .flatMap(detail -> detail.getBookingServiceDetails().stream())
                .filter(service -> !service.getIsPaid())
                .toList();

        List<UnpaidServiceItemResponse> unpaidServiceItemResponseList = unpaidServices.stream()
                .map(v -> UnpaidServiceItemResponse.builder()
                        .bookingServiceId(v.getId())
                        .serviceName(v.getService() != null ? v.getService().getName() : null) // Thay tên hàm get tùy theo Entity Service của bạn (ví dụ: getName())
                        .quantity(v.getQuantity())
                        .price(v.getPrice())
                        .subTotal(v.getPrice() != null ? v.getPrice() * v.getQuantity() : 0.0)
                        .usedAt(v.getUsedAt())
                        .build()
                )
                .toList();
        BigDecimal unpaidServiceTotal = unpaidServices.stream()
                .map(s -> BigDecimal.valueOf(s.getPrice()).multiply(BigDecimal.valueOf(s.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        return CheckoutSummaryResponse.builder()
                .orderId(order.getId())
                .unpaidServices(unpaidServiceItemResponseList)
                .remainingAmountToPay(unpaidServiceTotal) // Số tiền thực tế cần trả lúc checkout
                .build();
    }

    // ham add dich vu vao booking
    private List<BookingServiceDetail> processAndAttachServices(BookingDetail bookingDetail, List<BookingServiceRequest> serviceRequests) {
        if (serviceRequests == null || serviceRequests.isEmpty()) {
            return List.of();
        }

        return serviceRequests.stream().map(servReq -> {
            iuh.fit.se.hotelmanagement_be.modular.service.entities.Service service = serviceRepository.findById(servReq.getServiceId())
                    .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_FOUND));

            BigDecimal unitPrice = servReq.getPrice() != null ? BigDecimal.valueOf(servReq.getPrice()) : BigDecimal.valueOf(service.getPrice());
            LocalDateTime usageTime = servReq.getUsedAt() != null ? servReq.getUsedAt() : LocalDateTime.now();

            return BookingServiceDetail.builder()
                    .bookingDetail(bookingDetail)
                    .service(service)
                    .quantity(servReq.getQuantity())
                    .price(unitPrice.doubleValue())
                    .usedAt(usageTime)
                    .isPaid(false)
                    .build();
        }).collect(Collectors.toList());
    }

    private BigDecimal calculateTotalRoomPrice(List<BookingDetail> details) {
        return details.stream().map(d -> BigDecimal.valueOf(d.getRoomSubTotal())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalServicePrice(List<BookingDetail> bookingDetails) {
        if (bookingDetails == null) return BigDecimal.ZERO;

        return bookingDetails.stream()
                .filter(detail -> detail.getBookingServiceDetails() != null)
                .flatMap(detail -> detail.getBookingServiceDetails().stream())
                .map(serviceDetail -> BigDecimal.valueOf(serviceDetail.getPrice())
                        .multiply(BigDecimal.valueOf(serviceDetail.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal applyPromotion(Long customerId, String promoCode, BigDecimal currentTotalAmount,
                                      BigDecimal roomTotal, BigDecimal serviceTotal, Booking booking) {

        if (promoCode == null || promoCode.isEmpty()) {
            return BigDecimal.ZERO;
        }

        LocalDateTime now = LocalDateTime.now();

        var customerPromoOtp = customerPromotionRepository.findByUniqueCodeAndCustomerId(promoCode, customerId);
        if (customerPromoOtp.isPresent()) {
            CustomerPromotion customerPromotion = customerPromoOtp.get();

            if (customerPromotion.isUsed()) {
                throw new AppException(ErrorCode.PROMOTION_ALREADY_USED);
            }

            Promotion promotion = customerPromotion.getPromotion();
            validatePromotionRules(promotion, currentTotalAmount, now);

            customerPromotion.setUsed(true);
            customerPromotion.setUsedAt(now);
            customerPromotion.setBooking(booking);

            return calculateDiscountAmount(promotion, roomTotal, serviceTotal);
        }

        Promotion promotion = promotionRepository.findByCodeAndDeletedFalse(promoCode)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        validatePromotionRules(promotion, currentTotalAmount, now);
        promotion.setUsedCount(promotion.getUsedCount() + 1);

        return calculateDiscountAmount(promotion, roomTotal, serviceTotal);
    }

    private void validatePromotionRules(Promotion promotion, BigDecimal currentTotalAmount, LocalDateTime now) {
        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            throw new AppException(ErrorCode.PROMOTION_INACTIVE);
        }

        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            throw new AppException(ErrorCode.PROMOTION_EXPIRED);
        }

        if (promotion.getUsageLimit() != null && promotion.getUsedCount() >= promotion.getUsageLimit()) {
            throw new AppException(ErrorCode.PROMOTION_OUT_OF_STOCK);
        }

        if (promotion.getMinBookingValue() != null && currentTotalAmount.compareTo(promotion.getMinBookingValue()) < 0) {
            throw new AppException(ErrorCode.PROMOTION_MIN_ORDER_NOT_MET);
        }
    }

    private BigDecimal calculateDiscountAmount(Promotion promotion, BigDecimal roomTotal, BigDecimal serviceTotal) {
        BigDecimal discountAmount;
        // ap dung khuyen mai phai kiem tra xem don hang co duoc

        switch (promotion.getType()) {
            case ROOM_PERCENTAGE:
                discountAmount = roomTotal.multiply(promotion.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                break;

            case SERVICE_PERCENTAGE:
                discountAmount = serviceTotal.multiply(promotion.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                break;

            case TOTAL_PERCENTAGE:
                BigDecimal currentTotalAmount = roomTotal.add(serviceTotal);
                discountAmount = currentTotalAmount.multiply(promotion.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                break;

            default:
                discountAmount = BigDecimal.ZERO;
                break;
        }

        if (promotion.getMaxDiscountAmount() != null && discountAmount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
            discountAmount = promotion.getMaxDiscountAmount();
        }

        BigDecimal absoluteTotal = roomTotal.add(serviceTotal);
        return discountAmount.min(absoluteTotal);
    }


    @Transactional
    @Override
    public BookingResponse addServiceToExistingBooking(Long bookingId, List<BookingServiceRequest> serviceRequests) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        // Lấy BookingDetail đầu tiên (hoặc xử lý theo phòng cụ thể nếu nhiều phòng)
        BookingDetail targetDetail = booking.getBookingDetails().get(0);
        List<BookingServiceDetail> newServices = processAndAttachServices(targetDetail, serviceRequests);

        if (targetDetail.getBookingServiceDetails() == null) {
            targetDetail.setBookingServiceDetails(new ArrayList<>());
        }
        targetDetail.getBookingServiceDetails().addAll(newServices);

        // Tính tiền dịch vụ phát sinh mới thêm
        BigDecimal addedServiceTotal = newServices.stream()
                .map(s -> BigDecimal.valueOf(s.getPrice()).multiply(BigDecimal.valueOf(s.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // tien dich vu duoc cong vao dong nghia la cap nhat lai trang thai
        markServicesAsProcessedOrPaid(targetDetail.getBookingServiceDetails());
        // Cập nhật lại tổng tiền dịch vụ và tổng tiền của Order (Order vẫn giữ nguyên trạng thái OPEN)
        Order order = booking.getOrder();
        order.setServiceTotalAmount(order.getServiceTotalAmount().add(addedServiceTotal));

        Booking saved = bookingRepository.save(booking);
        return toBookingResponse(saved);
    }
}