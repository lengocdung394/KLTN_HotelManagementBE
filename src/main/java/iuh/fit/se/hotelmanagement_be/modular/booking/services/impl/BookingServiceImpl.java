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
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingDetailResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;
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
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import iuh.fit.se.hotelmanagement_be.modular.service.repositories.ServiceRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingServiceImpl {


    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final RoomRepository roomRepository;
    private final BranchRoomPolicyRepository branchRoomPolicyRepository;
    private final RoomPricingCalculator roomPricingCalculator;
    private final ServiceRepository serviceRepository;
    private final BookingRepository bookingRepository;
    private final CustomerPromotionRepository customerPromotionRepository;
    private final PromotionRepository promotionRepository;

    /**
     * LUỒNG CHÍNH 1: Khách hàng đặt online
     */
    @Transactional
    public BookingResponse createCustomerBooking(BookingCreateRequest request) {
        Customer customer = validateAndGetCustomer(request.getCustomerId());
        Booking booking = initBookingForCustomer(customer, BookingChannel.ONLINE, BookingStatus.PENDING);

        List<BookingDetail> details = processBookingDetails(booking, request.getBookingDetails());
        booking.setBookingDetails(details);

        // Gom chung logic tính toán giá và voucher vào helper method bên dưới
        Order order = calculateAndBuildOrder(customer.getId(), request, details, booking);
        booking.setOrder(order);

        Booking savedBooking = bookingRepository.save(booking);
        return toBookingResponse(savedBooking);
    }

    /**
     * LUỒNG CHÍNH 2: Nhân viên đặt tại quầy
     */
    @Transactional
    public BookingResponse createCounterBooking(Long employeeId, BookingCreateRequest request) {
        Customer customer = validateAndGetCustomer(request.getCustomerId());
        Employee employee = validateAndGetEmployee(employeeId);
        Booking booking = initBookingForEmployee(customer, employee, BookingChannel.OFFLINE, BookingStatus.CONFIRMED);

        List<BookingDetail> details = processBookingDetails(booking, request.getBookingDetails());
        booking.setBookingDetails(details);

        // Sử dụng chung helper method tính tiền và voucher
        Order order = calculateAndBuildOrder(customer.getId(), request, details, booking);
        booking.setOrder(order);

        Booking savedBooking = bookingRepository.save(booking);
        return toBookingResponse(savedBooking);
    }

    /**
     * HELPER METHOD: Gom toàn bộ logic tính tiền phòng, dịch vụ, và áp dụng voucher để tránh lặp code
     */
    private Order calculateAndBuildOrder(Long customerId, BookingCreateRequest request, List<BookingDetail> details, Booking booking) {
        BigDecimal roomTotal = calculateTotalRoomPrice(details);
        BigDecimal serviceTotal = calculateTotalServicePrice(details);
        BigDecimal currentSubTotal = roomTotal.add(serviceTotal);
        BigDecimal discountTotal = BigDecimal.ZERO;

        // 1. Kiểm tra mã khuyến mãi chung của khách sạn
        if (request.getPromotionId() != null) {
            Promotion p = promotionRepository.findById(request.getPromotionId())
                    .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
            discountTotal = applyPromotion(customerId, p.getCode(), currentSubTotal, roomTotal, serviceTotal, booking); // Truyền booking sau hoặc xử lý tùy ý
        }
        // 2. Kiểm tra mã cá nhân trong ví của khách
        else if (request.getCustomerPromotionId() != null) {
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
                .employee(employee) // Gắn nhân viên lập đơn tại quầy
                .bookingChannel(channel)
                .bookingStatus(status)
                .build();
    }

    private BookingResponse toBookingResponse(Booking booking) {
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
                            .numInfants(detail.getNumInfants())
                            .price(detail.getPrice())
                            .build()
            ).toList();
        }
        Order order = booking.getOrder();
        return BookingResponse.builder()
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

    private List<BookingDetail> processBookingDetails(Booking booking, List<BookingDetailCreateRequest> detailRequests) {
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

            double extraFeePerNight = roomPricingCalculator.calculateExtraFeeWithCapacityWeight(
                    policy,
                    detailReq.getNumAdults(),
                    detailReq.getNumChildren(),
                    detailReq.getNumInfants()
            );

            long nights = ChronoUnit.DAYS.between(
                    detailReq.getCheckInTime().toLocalDate(),
                    detailReq.getCheckOutTime().toLocalDate()
            );
            if (nights == 0) nights = 1;

            double roomTotalPrice = (room.calculateTotalPrice() + extraFeePerNight) * nights;

            // 1. Khởi tạo BookingDetail trước
            BookingDetail bookingDetail = BookingDetail.builder()
                    .booking(booking)
                    .room(room)
                    .checkinTime(detailReq.getCheckInTime())
                    .checkoutTime(detailReq.getCheckOutTime())
                    .numAdults(detailReq.getNumAdults())
                    .numChildren(detailReq.getNumChildren())
                    .numInfants(detailReq.getNumInfants())
                    .price(roomTotalPrice)
                    .build();

            // 2. Gọi hàm helper dùng chung để gán dịch vụ (nếu có)
            List<BookingServiceDetail> serviceDetails = processAndAttachServices(bookingDetail, detailReq.getServiceRequests());
            if (!serviceDetails.isEmpty()) {
                bookingDetail.setBookingServiceDetails(serviceDetails);
            }

            booking.addBookingDetail(bookingDetail);
            return bookingDetail;
        }).toList();
    }


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
                    .bookingDetail(bookingDetail) // Gắn trực tiếp vào BookingDetail tương ứng
                    .service(service)
                    .quantity(servReq.getQuantity())
                    .price(unitPrice.doubleValue())
                    .usedAt(usageTime)
                    .build();
        }).collect(Collectors.toList());
    }

    private BigDecimal calculateTotalRoomPrice(List<BookingDetail> details) {
        return details.stream().map(d -> BigDecimal.valueOf(d.getPrice())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    private List<BookingServiceDetail> processBookingServices(List<BookingDetail> createdBookingDetails, List<BookingServiceRequest> serviceRequests) {
        if (serviceRequests == null || serviceRequests.isEmpty()) {
            return List.of();
        }

        // Giả định: Dịch vụ có thể được gắn vào BookingDetail đầu tiên hoặc theo logic riêng của bạn
        BookingDetail targetBookingDetail = createdBookingDetails.get(0);

        return serviceRequests.stream().map(req -> {
            // 1. Kiểm tra dịch vụ có tồn tại trong hệ thống hay không
            iuh.fit.se.hotelmanagement_be.modular.service.entities.Service service = serviceRepository.findById(req.getServiceId())
                    .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_FOUND));

            // 2. Xác định đơn giá
            BigDecimal unitPrice;
            if (req.getPrice() != null) {
                unitPrice = BigDecimal.valueOf(req.getPrice());
            } else {
                unitPrice = BigDecimal.valueOf(service.getPrice());
            }

            // 3. Thời gian sử dụng
            LocalDateTime usageTime = req.getUsedAt() != null ? req.getUsedAt() : LocalDateTime.now();

            // 4. Tạo đối tượng BookingServiceDetail trỏ đúng vào BookingDetail
            BookingServiceDetail bookingServiceDetail = BookingServiceDetail.builder()
                    .bookingDetail(targetBookingDetail) // Truyền BookingDetail vào đây thay vì Booking!
                    .service(service)
                    .quantity(req.getQuantity())
                    .price(unitPrice.doubleValue())
                    .usedAt(usageTime)
                    .build();

            return bookingServiceDetail;
        }).toList();
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

        // Luong 1 xu li ma doc quyen
        var customerPromoOtp = customerPromotionRepository.findByUniqueCodeAndCustomerId(promoCode, customerId);
        if (customerPromoOtp.isPresent()) {
            CustomerPromotion customerPromotion = customerPromoOtp.get();

            // kiem tra xem da duoc su dung chua
            if (customerPromotion.isUsed()) {
                throw new AppException(ErrorCode.PROMOTION_ALREADY_USED);
            }

            Promotion promotion = customerPromotion.getPromotion();
            validatePromotionRules(promotion, currentTotalAmount, now);

            // Đánh dấu mã cá nhân này đã được sử dụng và khóa vào Booking
            customerPromotion.setUsed(true);
            customerPromotion.setUsedAt(now);
            customerPromotion.setBooking(booking);

            // Truyền đầy đủ roomTotal và serviceTotal vào hàm tính toán mới
            return calculateDiscountAmount(promotion, roomTotal, serviceTotal);
        }

        // Luong 2: Xử lý mã chung của khách sạn
        Promotion promotion = promotionRepository.findByCodeAndDeletedFalse(promoCode)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        validatePromotionRules(promotion, currentTotalAmount, now);

        // Tăng số lượng đã sử dụng của mã chung lên
        promotion.setUsedCount(promotion.getUsedCount() + 1);

        // Truyền đầy đủ roomTotal và serviceTotal vào hàm tính toán mới
        return calculateDiscountAmount(promotion, roomTotal, serviceTotal);
    }

    private void validatePromotionRules(Promotion promotion, BigDecimal currentTotalAmount, LocalDateTime now) {
        // Kiểm tra trạng thái ACTIVE
        if (promotion.getStatus() != PromotionStatus.ACTIVE) { // Đảm bảo bạn đã có enum PromotionStatus
            throw new AppException(ErrorCode.PROMOTION_INACTIVE);
        }

        // Kiểm tra thời hạn
        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            throw new AppException(ErrorCode.PROMOTION_EXPIRED);
        }

        // Kiểm tra số lượng lượt dùng tối đa (usageLimit)
        if (promotion.getUsageLimit() != null && promotion.getUsedCount() >= promotion.getUsageLimit()) {
            throw new AppException(ErrorCode.PROMOTION_OUT_OF_STOCK);
        }

        // Kiểm tra giá trị đơn tối thiểu (minBookingValue)
        if (promotion.getMinBookingValue() != null && currentTotalAmount.compareTo(promotion.getMinBookingValue()) < 0) {
            throw new AppException(ErrorCode.PROMOTION_MIN_ORDER_NOT_MET);
        }
    }

    /**
     * Hàm tính toán số tiền giảm dựa trên phạm vi áp dụng (Phòng, Dịch vụ hoặc Cả hai)
     */
    private BigDecimal calculateDiscountAmount(Promotion promotion, BigDecimal roomTotal, BigDecimal serviceTotal) {
        BigDecimal discountAmount = BigDecimal.ZERO;

        switch (promotion.getType()) {
            case ROOM_PERCENTAGE:
                // Chỉ giảm trên tổng tiền phòng
                discountAmount = roomTotal.multiply(promotion.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                break;

            case SERVICE_PERCENTAGE:
                // Chỉ giảm trên tổng tiền dịch vụ
                discountAmount = serviceTotal.multiply(promotion.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                break;

            case TOTAL_PERCENTAGE:
                // Giảm trên tổng cả phòng và dịch vụ
                BigDecimal currentTotalAmount = roomTotal.add(serviceTotal);
                discountAmount = currentTotalAmount.multiply(promotion.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                break;

            default:
                discountAmount = BigDecimal.ZERO;
                break;
        }

        // Kiểm tra trần giảm giá tối đa (maxDiscountAmount) nếu nhà quản lý có cấu hình giới hạn
        if (promotion.getMaxDiscountAmount() != null && discountAmount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
            discountAmount = promotion.getMaxDiscountAmount();
        }

        // Đảm bảo số tiền giảm không vượt quá tổng tiền thực tế của đơn hàng (Phòng + Dịch vụ)
        BigDecimal absoluteTotal = roomTotal.add(serviceTotal);
        return discountAmount.min(absoluteTotal);
    }

}
