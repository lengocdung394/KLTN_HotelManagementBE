package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.CustomerRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.impl.CustomerSocketEmitter;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingServiceDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingDetailRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingDetailCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingServiceRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.*;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingService;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.CustomerPromotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.Promotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionDiscountType;
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
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
    HotelRepository hotelRepository;
    RoomSeasonalRateRepository roomSeasonalRateRepository;
    BookingDetailRepository bookingDetailRepository;
    BookingSocketEmitter bookingSocketEmitter;
    private final CustomerSocketEmitter customerSocketEmitter;

    /**
     * LUỒNG CHÍNH 1: Khách hàng đặt online
     */
    @Transactional
    @Override
    public BookingResponse createCustomerBooking(BookingCreateRequest request) {
        Customer customer = validateAndGetCustomer(request.getCustomerId());
        Booking booking = initBookingForCustomer(customer, BookingChannel.ONLINE, BookingStatus.PENDING);

        // 1. Xử lý danh sách chi tiết đặt phòng
        List<BookingDetail> details = processBookingDetails(booking, request.getBookingDetails());

        // Đảm bảo clear và addAll để Hibernate quản lý collection chính xác, tránh lỗi flush
        booking.getBookingDetails().clear();
        if (details != null) {
            for (BookingDetail detail : details) {
                detail.setBooking(booking); // Thiết lập quan hệ 2 chiều bắt buộc cho JPA
                booking.getBookingDetails().add(detail);
            }
        }

        // 2. Tìm khách sạn an toàn (dùng orElseThrow thay vì .get())
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND));
        booking.setHotel(hotel);

        // 3. Tính toán và tạo Order
        Order order = calculateAndBuildOrder(customer.getId(), request, booking.getBookingDetails(), booking);
        booking.setOrder(order);

        // 4. Lưu vào Database
        Booking savedBooking = bookingRepository.save(booking);

        // === 5. BẮN SOCKET THÔNG BÁO REALTIME ===
        Long hotelId = hotel.getId();
        String customerId = customer.getId();

        // Báo cho nhân viên chi nhánh cập nhật lịch phòng & hiện thông báo đơn mới
        bookingSocketEmitter.emitRoomMatrixUpdate(hotelId);
        bookingSocketEmitter.emitNewBookingNotification(hotelId, savedBooking);


        // Báo về cho khách hàng trạng thái đơn hàng
        bookingSocketEmitter.emitCustomerBookingStatus(customerId, savedBooking);
        // ==========================================

        return toBookingResponse(savedBooking);
    }

    /**
     * LUỒNG CHÍNH 2: Nhân viên đặt tại quầy
     */
    @Transactional
    @Override
    public BookingResponse createCounterBooking(String employeeId, Long hotelId, BookingCreateRequest request) {
        Customer customer = validateAndGetCustomer(request.getCustomerId());
        Employee employee = validateAndGetEmployee(employeeId);
        Booking booking = initBookingForEmployee(customer, employee, BookingChannel.OFFLINE, BookingStatus.PENDING);

        // 1. Xử lý danh sách chi tiết đặt phòng
        List<BookingDetail> details = processBookingDetails(booking, request.getBookingDetails());

        // Đảm bảo clear và addAll để Hibernate quản lý collection chính xác
        booking.getBookingDetails().clear();
        if (details != null) {
            for (BookingDetail detail : details) {
                detail.setBooking(booking); // Thiết lập quan hệ 2 chiều bắt buộc cho JPA
                booking.getBookingDetails().add(detail);
            }
        }

        // 2. Tìm khách sạn
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND));
        booking.setHotel(hotel);

        // 3. Tính toán và tạo Order
        Order order = calculateAndBuildOrder(customer.getId(), request, booking.getBookingDetails(), booking);
        booking.setOrder(order);

        // 4. Lưu vào Database
        Booking savedBooking = bookingRepository.save(booking);

        // Sửa lại cú pháp log chuẩn SLF4J
        log.info("Nhan vien Tao booking: {}", order);

        // === 5. BẮN SOCKET THÔNG BÁO REALTIME CHO CHI NHÁNH ===
        if (hotelId != null) {
            bookingSocketEmitter.emitRoomMatrixUpdate(hotelId);
            bookingSocketEmitter.emitNewBookingNotification(hotelId, savedBooking);
        }
        // ====================================================

        return toBookingResponse(savedBooking);
    }

    /**
     * HELPER METHOD: Gom toàn bộ logic tính tiền phòng, dịch vụ, và áp dụng voucher
     */
    private Order calculateAndBuildOrder(String customerId, BookingCreateRequest request, List<BookingDetail> details, Booking booking) {
        BigDecimal roomTotal = calculateTotalRoomPrice(details);
        BigDecimal serviceTotal = calculateTotalServicePrice(details);
        BigDecimal currentSubTotal = roomTotal.add(serviceTotal);

        // 1. Khởi tạo kết quả giảm giá mặc định (bằng 0 cho tất cả các cột)
        PromotionDiscountResult discountResult = PromotionDiscountResult.builder()
                .discountRoomAmount(BigDecimal.ZERO)
                .discountServiceAmount(BigDecimal.ZERO)
                .discountAmountTotal(BigDecimal.ZERO)
                .build();

        // 2. Xử lý áp dụng mã khuyến mãi của chi nhánh hoặc cá nhân khách hàng
        if (request.getPromotionId() != null) {
            // Khuyến mãi cho chi nhánh / toàn hệ thống
            Promotion p = promotionRepository.findById(request.getPromotionId())
                    .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

            discountResult = applyPromotion(customerId, p.getCode(), currentSubTotal, roomTotal, serviceTotal, booking);

        } else if (request.getCustomerPromotionId() != null) {
            // Khuyến mãi riêng của cá nhân khách hàng
            CustomerPromotion customerPromo = customerPromotionRepository.findById(request.getCustomerPromotionId())
                    .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

            if (!customerPromo.getCustomer().getId().equals(customerId)) {
                throw new AppException(ErrorCode.UNAUTHORIZED_PROMOTION);
            }

            discountResult = applyPromotion(customerId, customerPromo.getUniqueCode(), currentSubTotal, roomTotal, serviceTotal, booking);
        }

        // 3. Truyền kết quả phân tách chi tiết vào hàm tạo Order
        return createAndLinkOrder(roomTotal, serviceTotal, discountResult);
    }

    private Customer validateAndGetCustomer(String customerId) {
        return customerRepository.findById(customerId).orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
    }

    private Employee validateAndGetEmployee(String employeeId) {
        return employeeRepository.findById(employeeId).orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_FOUND));
    }

    private Booking initBookingForCustomer(Customer customer, BookingChannel channel, BookingStatus status) {
        return Booking.
                builder()
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
            detailResponses = booking.getBookingDetails().stream().map(detail -> BookingDetailResponse.builder().bookingDetailId(detail.getId()).roomId(detail.getRoom() != null ? detail.getRoom().getId() : null).roomName(detail.getRoom() != null ? detail.getRoom().getRoomType().toString() : null).roomTypeName(detail.getRoom() != null ? detail.getRoom().getRoomType().name() : null).checkInTime(detail.getCheckinTime()).checkOutTime(detail.getCheckoutTime()).numAdults(detail.getNumAdults()).numChildren(detail.getNumChildren())
                    // Map thẳng các khoản chi tiết vào Response
                    .baseRoomPricePerNight(detail.getBaseRoomPricePerNight()).extraAdultFeePerNight(detail.getExtraAdultFeePerNight()).extraChildFeePerNight(detail.getExtraChildFeePerNight()).roomSubTotal(detail.getRoomSubTotal()).serviceSubTotal(detail.getServiceSubTotal()).totalPrice(detail.getTotalPrice()).build()).toList();
        }
        Order order = booking.getOrder();
        return BookingResponse
                .builder()
                .hotelId(booking.getHotel().getId()).orderId(order.getId()).bookingId(booking.getId())
                .customerId(booking.getCustomer() != null ? booking.getCustomer().getId() : null)
                .customerName(booking.getCustomer() != null ? booking.getCustomer().getFullName() : null)
                .bookingStatus(booking.getBookingStatus())
                .bookingChannel(booking.getBookingChannel())
                .createdAt(booking.getCreatedAt())
                .roomTotal(order != null ? order.getRoomTotalAmount() : null).serviceTotal(order != null ? order.getServiceTotalAmount() : null)
                .discountTotal(order != null ? order.getDiscountAmountTotal() : null).finalAmount(order != null ? order.getTotalAmount() : null)
                .discountServiceAmount(order != null ? order.getDiscountServiceAmount() : null)
                .discountRoomAmount(order != null ? order.getDiscountRoomAmount() : null)
                .discountServiceAmount(order != null ? order.getDiscountServiceAmount() : null)
                .bookingDetails(detailResponses).build();
    }

    private Order createAndLinkOrder(BigDecimal roomTotal, BigDecimal serviceTotal, PromotionDiscountResult discountResult) {

        // Đảm bảo không bị null pointer nếu lỡ object trả về bị trống
        BigDecimal roomDiscount = discountResult != null && discountResult.getDiscountRoomAmount() != null
                ? discountResult.getDiscountRoomAmount() : BigDecimal.ZERO;

        BigDecimal serviceDiscount = discountResult != null && discountResult.getDiscountServiceAmount() != null
                ? discountResult.getDiscountServiceAmount() : BigDecimal.ZERO;

        BigDecimal totalDiscount = discountResult != null && discountResult.getDiscountAmountTotal() != null
                ? discountResult.getDiscountAmountTotal() : BigDecimal.ZERO;

        // Tính toán tổng tiền thực tế khách cần phải trả sau khi đã trừ các khoản giảm giá
        // (Công thức: Tổng tiền phòng + Tổng tiền dịch vụ - Các khoản giảm giá tương ứng)
        BigDecimal subTotal = roomTotal.add(serviceTotal);
        BigDecimal totalDiscountSum = roomDiscount.add(serviceDiscount).add(totalDiscount);
        BigDecimal finalTotalAmount = subTotal.subtract(totalDiscountSum);

        // Đảm bảo tổng tiền không bị âm
        if (finalTotalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalTotalAmount = BigDecimal.ZERO;
        }

        return Order.builder()
                .issueDate(LocalDateTime.now())
                .roomTotalAmount(roomTotal)
                .serviceTotalAmount(serviceTotal)

                // Ghi chính xác từng loại tiền giảm vào đúng cột phân tách trong Order
                .discountRoomAmount(roomDiscount)
                .discountServiceAmount(serviceDiscount)
                .discountAmountTotal(totalDiscount)

                .totalAmount(finalTotalAmount) // Tổng tiền khách phải trả cuối cùng
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
            Room room = roomRepository.findById(detailReq.getRoomId()).orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

            Long hotelId = room.getFloor().getBuilding().getHotel().getId();
            BranchRoomPolicy policy = branchRoomPolicyRepository.findByHotelIdAndRoomType(hotelId, room.getRoomType());

            if (policy == null) {
                throw new AppException(ErrorCode.BRANCH_POLICY_NOT_FOUND);
            }

            // 1. Tính tiền phụ thu (người lớn/trẻ em) cho 1 đêm từ Policy
            ExtraFeeBreakdownResponse extraFeeBreakdown = roomPricingCalculator.calculateExtraFeeBreakdown(policy, detailReq.getNumAdults(), detailReq.getNumChildren());

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
                Optional<RoomSeasonalRate> seasonalRateOpt = roomSeasonalRateRepository.findActiveRateByDate(hotelId, room.getRoomType(), currentDate);

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
            double serviceSubTotal = serviceDetails.stream().filter(sd -> !Boolean.TRUE.equals(sd.getIsPaid())).mapToDouble(sd -> sd.getPrice() * sd.getQuantity()).sum(); //

            // cap nhat trang thai dich vu da duoc cong tien
            markServicesAsProcessedOrPaid(serviceDetails);
            // 5. Tổng cộng cuối cùng của phòng này (Phòng + Dịch vụ)
            double totalPrice = roomSubTotal + serviceSubTotal;

            // 6. Xây dựng Entity BookingDetail đầy đủ các khoản chi tiết
            BookingDetail bookingDetail = BookingDetail.builder().booking(booking).room(room).checkinTime(detailReq.getCheckInTime()).checkoutTime(detailReq.getCheckOutTime()).numAdults(detailReq.getNumAdults()).numChildren(detailReq.getNumChildren()).baseRoomPricePerNight(baseRoomPricePerNight).extraAdultFeePerNight(extraAdultFeePerNight).extraChildFeePerNight(extraChildFeePerNight).roomSubTotal(roomSubTotal).serviceSubTotal(serviceSubTotal).totalPrice(totalPrice).status(BookingStatusType.PENDING).build();

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
    public CheckoutSummaryResponse getCheckoutSummary(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId).get();
        Order order = booking.getOrder();

        // Lọc ra tất cả các dịch vụ CHƯA thanh toán (isPaid = false)
        List<BookingServiceDetail> unpaidServices = booking.getBookingDetails().stream().flatMap(detail -> detail.getBookingServiceDetails().stream()).filter(service -> !service.getIsPaid()).toList();

        List<UnpaidServiceItemResponse> unpaidServiceItemResponseList = unpaidServices.stream().map(v -> UnpaidServiceItemResponse.builder().bookingServiceId(v.getId()).serviceName(v.getService() != null ? v.getService().getName() : null) // Thay tên hàm get tùy theo Entity Service của bạn (ví dụ: getName())
                .quantity(v.getQuantity()).price(v.getPrice()).subTotal(v.getPrice() != null ? v.getPrice() * v.getQuantity() : 0.0).usedAt(v.getUsedAt()).build()).toList();
        BigDecimal unpaidServiceTotal = unpaidServices.stream().map(s -> BigDecimal.valueOf(s.getPrice()).multiply(BigDecimal.valueOf(s.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);


        return CheckoutSummaryResponse.builder().orderId(order.getId()).unpaidServices(unpaidServiceItemResponseList).remainingAmountToPay(unpaidServiceTotal) // Số tiền thực tế cần trả lúc checkout
                .build();
    }

    // ham add dich vu vao booking
    private List<BookingServiceDetail> processAndAttachServices(BookingDetail bookingDetail, List<BookingServiceRequest> serviceRequests) {
        if (serviceRequests == null || serviceRequests.isEmpty()) {
            return List.of();
        }

        return serviceRequests.stream().map(servReq -> {
            iuh.fit.se.hotelmanagement_be.modular.service.entities.Service service = serviceRepository.findById(servReq.getServiceId()).orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_FOUND));

            BigDecimal unitPrice = servReq.getPrice() != null ? BigDecimal.valueOf(servReq.getPrice()) : BigDecimal.valueOf(service.getPrice());
            LocalDateTime usageTime = servReq.getUsedAt() != null ? servReq.getUsedAt() : LocalDateTime.now();

            return BookingServiceDetail.builder().bookingDetail(bookingDetail).service(service).name(servReq.getName()).quantity(servReq.getQuantity()).price(unitPrice.doubleValue()).usedAt(usageTime).isPaid(false).build();
        }).collect(Collectors.toList());
    }

    private BigDecimal calculateTotalRoomPrice(List<BookingDetail> details) {
        return details.stream().map(d -> BigDecimal.valueOf(d.getRoomSubTotal())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalServicePrice(List<BookingDetail> bookingDetails) {
        if (bookingDetails == null) return BigDecimal.ZERO;

        return bookingDetails.stream().filter(detail -> detail.getBookingServiceDetails() != null).flatMap(detail -> detail.getBookingServiceDetails().stream()).map(serviceDetail -> BigDecimal.valueOf(serviceDetail.getPrice()).multiply(BigDecimal.valueOf(serviceDetail.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PromotionDiscountResult applyPromotion(String customerId, String promoCode, BigDecimal currentTotalAmount, BigDecimal roomTotal, BigDecimal serviceTotal, Booking booking) {

        // 1. Nếu không có mã khuyến mãi, trả về kết quả bằng 0 cho tất cả các cột
        if (promoCode == null || promoCode.trim().isEmpty()) {
            return PromotionDiscountResult.builder()
                    .discountRoomAmount(BigDecimal.ZERO)
                    .discountServiceAmount(BigDecimal.ZERO)
                    .discountAmountTotal(BigDecimal.ZERO)
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();

        var customerPromoOtp = customerPromotionRepository.findByUniqueCodeAndCustomerId(promoCode, customerId);
        if (customerPromoOtp.isPresent()) {
            // Tính toán cho khuyến mãi riêng của khách hàng
            CustomerPromotion customerPromotion = customerPromoOtp.get();

            if (customerPromotion.isUsed()) {
                throw new AppException(ErrorCode.PROMOTION_ALREADY_USED);
            }

            Promotion promotion = customerPromotion.getPromotion();
            validatePromotionRules(promotion, currentTotalAmount, now, roomTotal, serviceTotal);

            customerPromotion.setUsed(true);
            customerPromotion.setUsedAt(now);
            customerPromotion.setBooking(booking);

            return calculateDiscountBreakdown(promotion, roomTotal, serviceTotal);
        }

        // 2. Khuyến mãi chung của hệ thống / chi nhánh
        Promotion promotion = promotionRepository.findByCodeAndDeletedFalse(promoCode)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        validatePromotionRules(promotion, currentTotalAmount, now, roomTotal, serviceTotal);
        promotion.setUsedCount(promotion.getUsedCount() + 1);

        return calculateDiscountBreakdown(promotion, roomTotal, serviceTotal);
    }

    private void validatePromotionRules(Promotion promotion, BigDecimal currentTotalAmount, LocalDateTime now, BigDecimal roomTotal, BigDecimal serviceTotal) {
        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            throw new AppException(ErrorCode.PROMOTION_INACTIVE);
        }

        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            throw new AppException(ErrorCode.PROMOTION_EXPIRED);
        }

        if (promotion.getUsageLimit() != null && promotion.getUsedCount() >= promotion.getUsageLimit()) {
            throw new AppException(ErrorCode.PROMOTION_OUT_OF_STOCK);
        }

        // Kiểm tra ngưỡng tổng đơn tối thiểu
        if (promotion.getMinBookingValue() != null && currentTotalAmount.compareTo(promotion.getMinBookingValue()) < 0) {
            throw new AppException(ErrorCode.PROMOTION_MIN_ORDER_NOT_MET);
        }

        // Kiểm tra ngưỡng tiền phòng tối thiểu (MỚI)
        if (promotion.getMinRoomValue() != null && roomTotal.compareTo(promotion.getMinRoomValue()) < 0) {
            throw new AppException(ErrorCode.PROMOTION_MIN_ROOM_NOT_MET);
        }

        // Kiểm tra ngưỡng tiền dịch vụ tối thiểu (MỚI)
        if (promotion.getMinServiceValue() != null && serviceTotal.compareTo(promotion.getMinServiceValue()) < 0) {
            throw new AppException(ErrorCode.PROMOTION_MIN_SERVICE_NOT_MET);
        }
    }

    private PromotionDiscountResult calculateDiscountBreakdown(Promotion promotion, BigDecimal roomTotal, BigDecimal serviceTotal) {
        BigDecimal roomDiscount = BigDecimal.ZERO;
        BigDecimal serviceDiscount = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        roomTotal = roomTotal != null ? roomTotal : BigDecimal.ZERO;
        serviceTotal = serviceTotal != null ? serviceTotal : BigDecimal.ZERO;
        BigDecimal absoluteTotal = roomTotal.add(serviceTotal);

        // 1. Tính số tiền giảm thô dựa trên loại mã khuyến mãi
        BigDecimal rawDiscount = switch (promotion.getType()) {
            case ROOM -> calculateBaseDiscount(roomTotal, promotion);
            case SERVICE -> calculateBaseDiscount(serviceTotal, promotion);
            case TOTAL -> calculateBaseDiscount(absoluteTotal, promotion);
            default -> BigDecimal.ZERO;
        };

        // 2. Chặn trần giảm giá tối đa (maxDiscountAmount) nếu có cấu hình
        if (promotion.getMaxDiscountAmount() != null && rawDiscount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
            rawDiscount = promotion.getMaxDiscountAmount();
        }

        // 3. Đảm bảo tiền giảm không vượt quá tổng tiền thực tế của hóa đơn
        rawDiscount = rawDiscount.min(absoluteTotal);

        // 4. Phân tách rõ ràng vào các biến tương ứng tùy theo loại mã
        switch (promotion.getType()) {
            case ROOM:
                roomDiscount = rawDiscount;
                break;
            case SERVICE:
                serviceDiscount = rawDiscount;
                break;
            case TOTAL:
                totalDiscount = rawDiscount;
                break;
            default:
                break;
        }

        // Trả về đối tượng class chứa đầy đủ chi tiết từng khoản giảm
        return PromotionDiscountResult.builder()
                .discountRoomAmount(roomDiscount)
                .discountServiceAmount(serviceDiscount)
                .discountAmountTotal(totalDiscount)
                .build();
    }

    // Hàm phụ trợ để tính tiền giảm thô theo % hoặc số tiền mặt cố định
    private BigDecimal calculateBaseDiscount(BigDecimal baseAmount, Promotion promotion) {
        if (promotion.getDiscountType() == PromotionDiscountType.PERCENTAGE) {
            return baseAmount.multiply(promotion.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        } else if (promotion.getDiscountType() == PromotionDiscountType.FIXED_AMOUNT) {
            return promotion.getDiscountValue(); // Giảm số tiền mặt cố định (VD: giảm thẳng 50k)
        }
        return BigDecimal.ZERO;
    }

    @Override
    public BookingResponseForHotel toBookingForHotelResponse(Booking booking) {
        if (booking == null) {
            return null;
        }

        List<BookingDetailResponseForHotel> detailResponses = null;

        if (booking.getBookingDetails() != null) {
            detailResponses = booking.getBookingDetails().stream().map(detail -> BookingDetailResponseForHotel.builder().bookingStatusType(detail.getStatus()).cancelledAt(detail.getCancelledAt()).roomNumber(detail.getRoom().getRoomNumber()).bookingDetailId(detail.getId()).roomId(detail.getRoom() != null ? detail.getRoom().getId() : null).roomName(detail.getRoom() != null ? detail.getRoom().getRoomType().toString() : null).roomTypeName(detail.getRoom() != null ? detail.getRoom().getRoomType().name() : null).checkInTime(detail.getCheckinTime()).checkOutTime(detail.getCheckoutTime()).numAdults(detail.getNumAdults()).numChildren(detail.getNumChildren())
                    // Map thẳng các khoản chi tiết vào Response
                    .baseRoomPricePerNight(detail.getBaseRoomPricePerNight())
                    .extraAdultFeePerNight(detail.getExtraAdultFeePerNight())
                    .extraChildFeePerNight(detail.getExtraChildFeePerNight())
                    .roomSubTotal(detail.getRoomSubTotal())
                    .serviceSubTotal(detail.getServiceSubTotal())
                    .totalPrice(detail.getTotalPrice())
                    // map danh sach dich vu
                    .bookingServiceResponsForHotels(detail.getBookingServiceDetails() != null ? detail.getBookingServiceDetails().stream().map(serviceDetail -> BookingServiceResponseForHotel.builder().serviceId(serviceDetail.getService() != null ? serviceDetail.getService().getId() : null).name(serviceDetail.getName()).quantity(serviceDetail.getQuantity()).cancelled(serviceDetail.getCancelled()).cancelledAt(serviceDetail.getCancelledAt()).price(serviceDetail.getPrice()).usedAt(serviceDetail.getUsedAt()).build()).toList() : null).build()).toList();
        }
        Order order = booking.getOrder();
        return BookingResponseForHotel.builder()
                .orderId(order.getId())
                .bookingId(booking.getId())
                .customerId(booking.getCustomer() != null ? booking.getCustomer().getId() : null)
                .customerName(booking.getCustomer() != null ? booking.getCustomer().getFullName() : null)
                .bookingStatus(booking.getBookingStatus()).bookingChannel(booking.getBookingChannel())
                .createdAt(booking.getCreatedAt()).roomTotal(order != null ? order.getRoomTotalAmount() : null)
                .serviceTotal(order != null ? order.getServiceTotalAmount() : null)

                .discountTotal(order != null ? order.getDiscountAmountTotal() : null) // cai nay la cai field lay tien giam gia ( dinh nghia chung)
                .paidAmount(order != null ? order.getPaidAmount() : null) // tien da thanh toán
                .remainingAmount(order != null ? order.getRemainingAmount() : null) // so tien con lai
                .finalAmount(order != null ? order.getTotalAmount() : null) // tien tong hoa don
                // lam ro phan tien giam gia cho phong / dich vu / ca phong va dich vu
                .discountServiceAmount(order != null ? order.getDiscountAmountTotal() : null) // giam tien cho phan dich vu
                .discountAmountTotal(order != null ? order.getDiscountAmountTotal() : null) // giam cho tien phong
                .discountAmountTotal(order != null ? order.getDiscountAmountTotal() : null) // giam cho tong hoa don

                .bookingDetails(detailResponses).build();
    }

    @Override
    public List<BookingResponseForHotel> getBookingsByHotel(Long hotelId) {
        // Gọi repository lấy danh sách booking theo hotelId
        List<Booking> bookings = bookingRepository.findAllByHotelId(hotelId);

        // Chuyển đổi sang danh sách BookingResponse
        return bookings.stream().map(this::toBookingForHotelResponse).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public List<RoomMatrixResponse> getRoomMatrix(Long hotelId, LocalDate startDate, LocalDate endDate) {
        List<Room> allRooms = roomRepository.findByFloor_Building_Hotel_Id(hotelId);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        List<BookingDetail> activeDetails = bookingDetailRepository.findActiveBookingsByDateRange(
                hotelId, startDateTime, endDateTime, fiveMinutesAgo
        );

        Map<String, List<BookingDetail>> roomSchedulesMap = activeDetails.stream()
                .collect(Collectors.groupingBy(detail -> detail.getRoom().getId()));

        return allRooms.stream().map(room -> {
            List<BookingDetail> detailsForRoom = roomSchedulesMap.getOrDefault(room.getId(), Collections.emptyList());

            List<RoomScheduleDto> schedules = detailsForRoom.stream()
                    .filter(detail -> {
                        Booking booking = detail.getBooking();
                        if (booking == null) return true;

                        if (booking.getBookingStatus() == BookingStatus.PENDING
                                && booking.getCreatedAt() != null
                                && booking.getCreatedAt().isBefore(fiveMinutesAgo)) {
                            return false;
                        }

                        return true;
                    })
                    .map(detail -> {
                        List<LocalDate> dates = new ArrayList<>();
                        if (detail.getCheckinTime() != null && detail.getCheckoutTime() != null) {
                            LocalDate current = detail.getCheckinTime().toLocalDate();
                            LocalDate end = detail.getCheckoutTime().toLocalDate();
                            while (!current.isAfter(end)) {
                                dates.add(current);
                                current = current.plusDays(1);
                            }
                        }

                        return RoomScheduleDto.builder()
                                .bookingId(detail.getBooking() != null ? detail.getBooking().getId() : null)
                                .customerName(detail.getBooking() != null && detail.getBooking().getCustomer() != null
                                        ? detail.getBooking().getCustomer().getFullName()
                                        : "Khách tại quầy")
                                .checkinTime(detail.getCheckinTime())
                                .checkoutTime(detail.getCheckoutTime())
                                .bookingStatus(detail.getBooking() != null ? detail.getBooking().getBookingStatus() : null)
                                .occupiedDates(dates)
                                .build();
                    })
                    .collect(Collectors.toList());

            return RoomMatrixResponse.builder()
                    .roomId(room.getId())
                    .roomNumber(room.getRoomNumber())
                    .roomTypeName(room.getRoomType() != null ? room.getRoomType().name() : "Standard")
                    .schedules(schedules)
                    .build();
        }).collect(Collectors.toList());
    }
}