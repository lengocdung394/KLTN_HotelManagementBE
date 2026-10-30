package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.CustomerRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.impl.EmailService;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingNightlyPrice;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingServiceDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingCancellation;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingDetailRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingCancellationRepository;
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
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.PaymentRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.CustomerPromotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.Promotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionDiscountType;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionStatus;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.CustomerPromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.PromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.services.PromotionUsageService;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomSeasonalRate;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus;
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
    BookingCancellationRepository bookingCancellationRepository;
    BookingSocketEmitter bookingSocketEmitter;
    PromotionUsageService promotionUsageService;
    EmailService emailService;
    AccountRepository accountRepository;
    PaymentRepository paymentRepository;


    /**
     * LUá»’NG CHÃNH 1: KhÃ¡ch hÃ ng Ä‘áº·t online
     */
    @Transactional
    @Override
    public BookingResponse createCustomerBooking(BookingCreateRequest request) {
        Customer customer = validateAndGetCustomer(request.getCustomerId(), request.getCustomerEmail());
        ensureCustomerEmail(customer, request);
        Booking booking = initBookingForCustomer(customer, BookingChannel.ONLINE, BookingStatus.PENDING);
        // LÆ°u láº¡i tráº¡ng thÃ¡i cÃ³ dÃ¹ng mÃ£ ngay tá»« Ä‘áº§u hÃ m Ä‘á»ƒ trÃ¡nh bá»‹ thay Ä‘á»•i vá» sau
        boolean hasPromotion = (request.getCustomerPromotionId() != null || request.getPromotionId() != null);
        // 1. TÃ¬m khÃ¡ch sáº¡n an toÃ n vÃ  gÃ¡n vÃ o booking trÆ°á»›c
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND));
        booking.setHotel(hotel);

        // 2. Xá»­ lÃ½ danh sÃ¡ch chi tiáº¿t Ä‘áº·t phÃ²ng (dá»±a trÃªn hotel Ä‘Ã£ Ä‘Æ°á»£c xÃ¡c Ä‘á»‹nh)
        List<BookingDetail> details = processBookingDetails(booking, request.getBookingDetails());

        // Äáº£m báº£o clear vÃ  addAll Ä‘á»ƒ Hibernate quáº£n lÃ½ collection chÃ­nh xÃ¡c, trÃ¡nh lá»—i flush
        booking.getBookingDetails().clear();
        if (details != null) {
            for (BookingDetail detail : details) {
                detail.setBooking(booking); // Thiáº¿t láº­p quan há»‡ 2 chiá»u báº¯t buá»™c cho JPA
                booking.getBookingDetails().add(detail);
            }
        }

//        // 2. TÃ¬m khÃ¡ch sáº¡n an toÃ n (dÃ¹ng orElseThrow thay vÃ¬ .get())
//        Hotel hotel = hotelRepository.findById(request.getHotelId())
//                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND));
        booking.setHotel(hotel);

        // 3. TÃ­nh toÃ¡n vÃ  táº¡o Order
        Order order = calculateAndBuildOrder(customer.getId(), request, booking.getBookingDetails(), booking);
        booking.setOrder(order);

        // DÃ¹ng biáº¿n hasPromotion Ä‘Ã£ lÆ°u tá»« Ä‘áº§u, cháº¯c cháº¯n sáº½ khÃ´ng bá»‹ null oan uá»•ng
        booking.setApplyAt(hasPromotion ? LocalDateTime.now() : null);
        // 4. LÆ°u vÃ o Database
        Booking savedBooking = bookingRepository.save(booking);

        // logic cua khuyen mai
        promotionUsageService.replaceBookingPromotion(
                savedBooking,
                savedBooking.getCustomerPromotion() == null ? savedBooking.getPromotion() : savedBooking.getCustomerPromotion().getPromotion(),
                savedBooking.getCustomerPromotion()
        );

        // === 5. Báº®N SOCKET THÃ”NG BÃO REALTIME ===
        Long hotelId = hotel.getId();
        String customerId = customer.getId();

        // Báo cho nhân viên chi nhánh cập nhật lịch phòng & hiện thông báo đơn mới
        bookingSocketEmitter.emitRoomMatrixUpdate(hotelId);

        // CHá»ˆ báº¯n thÃ´ng bÃ¡o Ä‘Æ¡n má»›i tá»›i nhÃ¢n viÃªn vÃ  email khi booking ÄÃƒ ÄÆ¯á»¢C XÃC NHáº¬N / THANH TOÃN THÃ€NH CÃ”NG
        // ÄÆ¡n ONLINE Ä‘ang PENDING chá» thanh toÃ¡n sáº½ chá»‰ báº¯n thÃ´ng bÃ¡o sau khi PayOS/ngÃ¢n hÃ ng xÃ¡c nháº­n thanh toÃ¡n thÃ nh cÃ´ng
        if (savedBooking.getBookingStatus() == BookingStatus.CONFIRMED) {
            Map<String, Object> bookingData = new LinkedHashMap<>();
            bookingData.put("bookingId", savedBooking.getId());
            bookingData.put("customerName", customer.getFullName());
            bookingData.put("customerPhone", customer.getPhone());
            bookingData.put("totalAmount", savedBooking.getOrder().getTotalAmount());

            bookingSocketEmitter.emitNewBookingNotification(
                    hotelId,
                    savedBooking,
                    bookingData
            );
            // BÃ¡o vá» cho khÃ¡ch hÃ ng tráº¡ng thÃ¡i Ä‘Æ¡n hÃ ng
            // Báo về cho khách hàng trạng thái đơn hàng
            bookingSocketEmitter.emitCustomerBookingStatus(
                    customer,
                    savedBooking,
                    NotificationType.BOOKING_CREATED,
                    "Đặt phòng thành công",
                    "Mã booking: #" + savedBooking.getId()
                            + " | Khách hàng: " + customer.getFullName()
                            + " | SĐT: " + customer.getPhone()
                            + " | Tổng tiền booking: " + savedBooking.getOrder().getTotalAmount() + "đ.");
            sendBookingCreatedEmailAfterCommit(savedBooking, customer, hotel);
        }
        // ==========================================

        return toBookingResponse(savedBooking);
    }

    /**
     * LUá»’NG CHÃNH 2: NhÃ¢n viÃªn Ä‘áº·t táº¡i quáº§y
     */
    @Transactional
    @Override
    public BookingResponse createCounterBooking(String employeeId, Long hotelId, BookingCreateRequest request) {
        Customer customer = validateAndGetCustomer(request.getCustomerId(), request.getCustomerEmail());
        ensureCustomerEmail(customer, request);
        Employee employee = validateAndGetEmployee(employeeId);
        Booking booking = initBookingForEmployee(customer, employee, BookingChannel.OFFLINE, BookingStatus.PENDING);
        // LÆ°u láº¡i tráº¡ng thÃ¡i cÃ³ dÃ¹ng mÃ£ ngay tá»« Ä‘áº§u hÃ m Ä‘á»ƒ trÃ¡nh bá»‹ thay Ä‘á»•i vá» sau
        boolean hasPromotion = (request.getCustomerPromotionId() != null || request.getPromotionId() != null);
        // 1. Xá»­ lÃ½ danh sÃ¡ch chi tiáº¿t Ä‘áº·t phÃ²ng
        List<BookingDetail> details = processBookingDetails(booking, request.getBookingDetails());

        // Äáº£m báº£o clear vÃ  addAll Ä‘á»ƒ Hibernate quáº£n lÃ½ collection chÃ­nh xÃ¡c
        booking.getBookingDetails().clear();
        if (details != null) {
            for (BookingDetail detail : details) {
                detail.setBooking(booking); // Thiáº¿t láº­p quan há»‡ 2 chiá»u báº¯t buá»™c cho JPA
                booking.getBookingDetails().add(detail);
            }
        }

        // 2. TÃ¬m khÃ¡ch sáº¡n
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND));
        booking.setHotel(hotel);

        // DÃ¹ng biáº¿n hasPromotion Ä‘Ã£ lÆ°u tá»« Ä‘áº§u, cháº¯c cháº¯n sáº½ khÃ´ng bá»‹ null oan uá»•ng
        booking.setApplyAt(hasPromotion ? LocalDateTime.now() : null);
        // 3. TÃ­nh toÃ¡n vÃ  táº¡o Order
        Order order = calculateAndBuildOrder(customer.getId(), request, booking.getBookingDetails(), booking);
        booking.setOrder(order);

        // 4. LÆ°u vÃ o Database
        Booking savedBooking = bookingRepository.save(booking);
        promotionUsageService.replaceBookingPromotion(
                savedBooking,
                savedBooking.getCustomerPromotion() == null ? savedBooking.getPromotion() : savedBooking.getCustomerPromotion().getPromotion(),
                savedBooking.getCustomerPromotion()
        );

        // Sá»­a láº¡i cÃº phÃ¡p log chuáº©n SLF4J
        log.info("Nhan vien Tao booking: orderId={}, bookingId={}, promotionId={}",
                order.getId(),
                savedBooking.getId(),
                savedBooking.getPromotion() != null ? savedBooking.getPromotion().getId() : null);

        // === 5. Báº®N SOCKET THÃ”NG BÃO REALTIME CHO CHI NHÃNH ===
        if (savedBooking.getBookingStatus() == BookingStatus.CONFIRMED && hotelId != null) {
            bookingSocketEmitter.emitRoomMatrixUpdate(hotelId);
            Map<String, Object> bookingData = new LinkedHashMap<>();
            bookingData.put("bookingId", savedBooking.getId());
            bookingData.put("customerName", customer.getFullName());
            bookingData.put("customerPhone", customer.getPhone());
            bookingData.put("totalAmount", savedBooking.getOrder().getTotalAmount());

            bookingSocketEmitter.emitNewBookingNotification(
                    hotelId,
                    savedBooking,
                    bookingData
            );
            sendBookingCreatedEmailAfterCommit(savedBooking, customer, hotel);
        } else if (hotelId != null) {
            bookingSocketEmitter.emitRoomMatrixUpdate(hotelId);
        }
        // ====================================================

        return toBookingResponse(savedBooking);
    }

    private void sendBookingCreatedEmailAfterCommit(Booking booking, Customer customer, Hotel hotel) {
        String email = emailService.getCustomerEmail(customer);

        List<EmailService.BookingRoomEmailDetail> bookingDetails = booking.getBookingDetails().stream()
                .map(detail -> new EmailService.BookingRoomEmailDetail(
                        detail.getRoom() != null ? detail.getRoom().getRoomNumber() : null,
                        detail.getCheckinTime(),
                        detail.getCheckoutTime(),
                        detail.getBookingServiceDetails() == null
                                ? List.of()
                                : detail.getBookingServiceDetails().stream()
                                .filter(service -> !Boolean.TRUE.equals(service.getCancelled()))
                                .map(service -> new EmailService.BookingServiceEmailDetail(
                                        service.getName() != null
                                                ? service.getName()
                                                : service.getService() != null ? service.getService().getName() : null,
                                        service.getQuantity(),
                                        service.getPrice() != null
                                                ? BigDecimal.valueOf(service.getPrice())
                                                : BigDecimal.ZERO))
                                .toList()))
                .toList();
        String customerName = customer.getFullName();
        String bookingId = booking.getId();
        String hotelName = hotel.getName();
        BigDecimal totalAmount = booking.getOrder() != null
                ? booking.getOrder().getTotalAmount()
                : BigDecimal.ZERO;
        var customerPromotion = booking.getCustomerPromotion();
        var promotion = customerPromotion != null ? customerPromotion.getPromotion() : booking.getPromotion();
        String promotionName = promotion != null ? promotion.getName() : null;
        String promotionCode = customerPromotion != null
                ? customerPromotion.getUniqueCode()
                : promotion != null ? promotion.getCode() : null;
        BigDecimal promotionDiscount = booking.getApplyAmount() != null
                ? booking.getApplyAmount()
                : BigDecimal.ZERO;
        boolean paymentDueWithinFiveMinutes = booking.getBookingStatus() == BookingStatus.PENDING;

        emailService.sendAfterCommit("booking " + bookingId, () ->
                emailService.sendBookingCreatedEmail(
                        email,
                        customerName,
                        bookingId,
                        hotelName,
                        bookingDetails,
                        totalAmount,
                        promotionName,
                        promotionCode,
                        promotionDiscount,
                        paymentDueWithinFiveMinutes
                ));
    }

    private void ensureCustomerEmail(Customer customer, BookingCreateRequest request) {
        String requestedEmail = request.getCustomerEmail();
        if (requestedEmail != null && !requestedEmail.isBlank()) {
            customer.setEmail(requestedEmail.trim());
            customerRepository.save(customer);
            return;
        }

        String customerEmail = emailService.getCustomerEmail(customer);
        if (customerEmail != null && !isFallbackCustomerEmail(customerEmail)) {
            return;
        }

        String accountEmail = customer.getAccount() != null ? customer.getAccount().getEmail() : null;
        if (accountEmail != null && !accountEmail.isBlank() && !isFallbackCustomerEmail(accountEmail)) {
            customer.setEmail(accountEmail.trim());
            customerRepository.save(customer);
            return;
        }

        throw new AppException(ErrorCode.CUSTOMER_EMAIL_REQUIRED);
    }

    private boolean isFallbackCustomerEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT).endsWith("@guest.senviet.vn");
    }

    private Order calculateAndBuildOrder(String customerId, BookingCreateRequest request, List<BookingDetail> details, Booking booking) {
        BigDecimal roomTotal = calculateTotalRoomPrice(details);
        BigDecimal serviceTotal = calculateTotalServicePrice(details);

        // TÃ­nh tá»•ng tiá»n phá»¥ thu tá»« táº¥t cáº£ cÃ¡c BookingDetail (earlyCheckInFee, lateCheckOutFee,...)
        BigDecimal surchargeTotal = details.stream()
                .map(d -> {
                    BigDecimal earlyFee = d.getEarlyCheckInFee() != null ? d.getEarlyCheckInFee() : BigDecimal.ZERO;
                    BigDecimal lateFee = d.getLateCheckOutFee() != null ? d.getLateCheckOutFee() : BigDecimal.ZERO;
                    BigDecimal otherFee = d.getOtherSurcharges() != null ? d.getOtherSurcharges() : BigDecimal.ZERO;
                    return earlyFee.add(lateFee).add(otherFee);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentSubTotal = roomTotal.add(serviceTotal).add(surchargeTotal);
        BigDecimal discountTotal = BigDecimal.ZERO;

        // 1. Khá»Ÿi táº¡o káº¿t quáº£ giáº£m giÃ¡ máº·c Ä‘á»‹nh (báº±ng 0 cho táº¥t cáº£ cÃ¡c cá»™t)
        PromotionDiscountResult discountResult = PromotionDiscountResult.builder()
                .discountRoomAmount(BigDecimal.ZERO)
                .discountServiceAmount(BigDecimal.ZERO)
                .discountAmountTotal(BigDecimal.ZERO)
                .build();

        // 2. Xá»­ lÃ½ Ã¡p dá»¥ng mÃ£ khuyáº¿n mÃ£i cá»§a chi nhÃ¡nh hoáº·c cÃ¡ nhÃ¢n khÃ¡ch hÃ ng
        if (request.getPromotionId() != null) {
            Promotion p = promotionRepository.findById(request.getPromotionId())
                    .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
            booking.setPromotion(p);
            discountResult = applyPromotion(customerId, p.getCode(), currentSubTotal, roomTotal, serviceTotal, booking);

        } else if (request.getCustomerPromotionId() != null) {
            CustomerPromotion customerPromo = customerPromotionRepository.findByIdAndCustomerIdForUpdate(
                            request.getCustomerPromotionId(),
                            customerId)
                    .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

            if (!customerPromo.getCustomer().getId().equals(customerId)) {
                throw new AppException(ErrorCode.UNAUTHORIZED_PROMOTION);
            }
            booking.setCustomerPromotion(customerPromo);
            discountResult = applyPromotion(customerId, customerPromo.getUniqueCode(), currentSubTotal, roomTotal, serviceTotal, booking);
        }

        booking.setApplyAmount(
                discountResult.getDiscountRoomAmount()
                        .add(discountResult.getDiscountServiceAmount())
                        .add(discountResult.getDiscountAmountTotal())
        );

        // 3. Truyá»n thÃªm surchargeTotal vÃ o hÃ m táº¡o Order
        return createAndLinkOrder(roomTotal, serviceTotal, discountResult, surchargeTotal);
    }

    private Customer validateAndGetCustomer(String customerId, String customerEmail) {
        if (customerId != null && !customerId.isBlank()) {
            Optional<Customer> opt = customerRepository.findById(customerId);
            if (opt.isPresent()) return opt.get();

            if (!customerId.startsWith("CUS_") && !customerId.startsWith("CUS")) {
                Optional<Customer> optPrefix = customerRepository.findById("CUS_" + customerId);
                if (optPrefix.isPresent()) return optPrefix.get();
            }
            Optional<Customer> optPhone = customerRepository.findFirstByPhone(customerId);
            if (optPhone.isPresent()) return optPhone.get();
        }

        if (customerEmail != null && !customerEmail.isBlank()) {
            Optional<Customer> optEmail = customerRepository.findFirstByEmail(customerEmail.trim());
            if (optEmail.isPresent()) return optEmail.get();

            Optional<Account> acc = accountRepository.findByEmail(customerEmail.trim());
            if (acc.isPresent()) {
                Optional<Customer> optAcc = customerRepository.findFirstByAccount(acc.get());
                if (optAcc.isPresent()) return optAcc.get();
            }
        }

        if (customerEmail != null && !customerEmail.isBlank()) {
            Customer newCustomer = Customer.builder()
                    .email(customerEmail.trim())
                    .fullName(customerEmail.split("@")[0])
                    .loyaltyTier(iuh.fit.se.hotelmanagement_be.modular.auth.entities.enums.LoyaltyTier.BRONZE)
                    .isRegistered(false)
                    .build();
            return customerRepository.save(newCustomer);
        }

        throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
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
            detailResponses = booking.getBookingDetails().stream()
                    .map(detail -> BookingDetailResponse.builder()
                            .bookingDetailId(detail.getId())
                            .roomId(detail.getRoom() != null ? detail.getRoom().getId() : null)
                            .roomName(detail.getRoom() != null ? detail.getRoom().getRoomType().toString() : null)
                            .roomTypeName(detail.getRoom() != null ? detail.getRoom().getRoomType().name() : null)
                            .checkInTime(detail.getCheckinTime()).checkOutTime(detail.getCheckoutTime())
                            .numAdults(detail.getNumAdults()).numChildren(detail.getNumChildren())
                            .earlyCheckInFee(detail.getEarlyCheckInFee())
                            .lateCheckOutFee(detail.getLateCheckOutFee())
                            // Map tháº³ng cÃ¡c khoáº£n chi tiáº¿t vÃ o Response
                            .baseRoomPricePerNight(detail.getBaseRoomPricePerNight()).extraAdultFeePerNight(detail.getExtraAdultFeePerNight()).extraChildFeePerNight(detail.getExtraChildFeePerNight()).roomSubTotal(detail.getRoomSubTotal()).serviceSubTotal(detail.getServiceSubTotal()).totalPrice(detail.getTotalPrice())
                            .nightlyPrices(detail.getNightlyPrices() == null ? List.of() : detail.getNightlyPrices().stream().map(price -> BookingNightlyPriceResponse.builder()
                                    .stayDate(price.getStayDate())
                                    .roomBasePrice(price.getRoomBasePrice())
                                    .extraAdultFee(price.getExtraAdultFee())
                                    .extraChildFee(price.getExtraChildFee())
                                    .extraAdultUnitPrice(price.getExtraAdultUnitPrice())
                                    .extraChildUnitPrice(price.getExtraChildUnitPrice())
                                    .standardCapacity(price.getStandardCapacity())
                                    .maxExtraGuests(price.getMaxExtraGuests())
                                    .nightTotal(price.getNightTotal())
                                    .build()).toList())
                            .build()).toList();
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

                .customerPromotionId(booking.getCustomerPromotion() != null ? booking.getCustomerPromotion().getId() : null)
                .promotionId(booking.getPromotion() != null ? booking.getPromotion().getId() : null)

                .surchargeTotalAmount(order.getSurchargeTotalAmount())

                .roomTotal(order != null ? order.getRoomTotalAmount() : null).serviceTotal(order != null ? order.getServiceTotalAmount() : null)

                .finalAmount(order != null ? order.getTotalAmount() : null)

                .discountTotal(order != null ? order.getDiscountAmountTotal() : null)
                .discountServiceAmount(order != null ? order.getDiscountServiceAmount() : null)
                .discountRoomAmount(order != null ? order.getDiscountRoomAmount() : null)
                .estimatedDiscountAmount(booking.getApplyAmount() != null
                        ? booking.getApplyAmount()
                        : order != null
                        ? (order.getDiscountAmountTotal() != null ? order.getDiscountAmountTotal() : BigDecimal.ZERO)
                                .add(order.getDiscountRoomAmount() != null ? order.getDiscountRoomAmount() : BigDecimal.ZERO)
                                .add(order.getDiscountServiceAmount() != null ? order.getDiscountServiceAmount() : BigDecimal.ZERO)
                        : BigDecimal.ZERO)
                .bookingDetails(detailResponses).build();
    }

    private Order createAndLinkOrder(BigDecimal roomTotal, BigDecimal serviceTotal, PromotionDiscountResult discountResult, BigDecimal surchargeTotal) {

        BigDecimal roomDiscount = discountResult != null && discountResult.getDiscountRoomAmount() != null
                ? discountResult.getDiscountRoomAmount() : BigDecimal.ZERO;

        BigDecimal serviceDiscount = discountResult != null && discountResult.getDiscountServiceAmount() != null
                ? discountResult.getDiscountServiceAmount() : BigDecimal.ZERO;

        BigDecimal totalDiscount = discountResult != null && discountResult.getDiscountAmountTotal() != null
                ? discountResult.getDiscountAmountTotal() : BigDecimal.ZERO;

        BigDecimal safeSurcharge = surchargeTotal != null ? surchargeTotal : BigDecimal.ZERO;

        // CÃ”NG THUÌC CHUáº¨N: (Tiá»n phÃ²ng + Tiá»n dá»‹ch vá»¥ + Phá»¥ thu) - Tá»•ng giáº£m giÃ¡
        BigDecimal subTotal = roomTotal.add(serviceTotal).add(safeSurcharge);
        BigDecimal totalDiscountSum = roomDiscount.add(serviceDiscount).add(totalDiscount);
        BigDecimal finalTotalAmount = subTotal.subtract(totalDiscountSum);

        if (finalTotalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalTotalAmount = BigDecimal.ZERO;
        }

        return Order.builder()
                .issueDate(LocalDateTime.now())
                .roomTotalAmount(roomTotal)
                .serviceTotalAmount(serviceTotal)
                .surchargeTotalAmount(safeSurcharge) // Ghi nháº­n tiá»n phá»¥ thu vÃ o Order
                .remainingAmount(finalTotalAmount)
                .discountRoomAmount(roomDiscount)
                .discountServiceAmount(serviceDiscount)
                .discountAmountTotal(totalDiscount)
                .totalAmount(finalTotalAmount)
                .paidAmount(BigDecimal.ZERO)
                .orderStatus(OrderStatusType.OPEN)
                .build();
    }

    @Override
    public List<BookingDetail> processBookingDetails(Booking booking, List<BookingDetailCreateRequest> detailRequests) {
        if (detailRequests == null || detailRequests.isEmpty()) {
            throw new AppException(ErrorCode.BOOKING_DETAILS_REQUIRED);
        }
        Long targetHotelId = booking.getHotel() != null ? booking.getHotel().getId() : null;
        Map<iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType, List<RoomSeasonalRate>> seasonalRatesByRoomType = new HashMap<>();
        if (targetHotelId != null) {
            List<BookingDetailCreateRequest> datedDetails = detailRequests.stream()
                    .filter(detail -> detail != null
                            && detail.getCheckInTime() != null
                            && detail.getCheckOutTime() != null
                            && detail.getCheckOutTime().isAfter(detail.getCheckInTime()))
                    .toList();
            if (!datedDetails.isEmpty()) {
                LocalDate firstStayDate = datedDetails.stream()
                        .map(detail -> detail.getCheckInTime().toLocalDate())
                        .min(LocalDate::compareTo)
                        .orElseThrow();
                LocalDate lastStayDate = datedDetails.stream()
                        .map(detail -> detail.getCheckOutTime().toLocalDate())
                        .max(LocalDate::compareTo)
                        .orElseThrow()
                        .minusDays(1);
                roomSeasonalRateRepository.findRatesByBranchAndDateRange(targetHotelId, firstStayDate, lastStayDate)
                        .forEach(rate -> seasonalRatesByRoomType
                                .computeIfAbsent(rate.getRoomType(), ignored -> new ArrayList<>())
                                .add(rate));
            }
        }
        Map<iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType, BranchRoomPolicy> policiesByRoomType = new HashMap<>();

        return detailRequests.stream().map(detailReq -> {
            String targetRoomId = detailReq.getRoomId();

            Room room = null;
            if (targetHotelId != null && targetRoomId != null && !targetRoomId.isBlank()) {
                room = roomRepository.findByIdAndFloor_Building_Hotel_Id(targetRoomId, targetHotelId)
                        .or(() -> roomRepository.findFirstByRoomNumberAndFloor_Building_Hotel_Id(targetRoomId, targetHotelId))
                        .orElse(null);
            }

            if (room == null && targetRoomId != null && !targetRoomId.isBlank()) {
                room = roomRepository.findById(targetRoomId)
                        .or(() -> roomRepository.findFirstByRoomNumber(targetRoomId))
                        .orElse(null);
            }

            // Náº¿u phÃ²ng khÃ´ng khá»›p hotel cá»§a booking, bá» Ä‘á»ƒ tÃ¬m phÃ²ng thuá»™c Ä‘Ãºng khÃ¡ch sáº¡n
            if (room != null && targetHotelId != null) {
                Long roomHotelId = room.getFloor().getBuilding().getHotel().getId();
                if (!roomHotelId.equals(targetHotelId)) {
                    room = null;
                }
            }

            // Never replace a requested room silently; report conflicts against the requested room.
            if (room != null) {
                LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
                long overlapping = bookingDetailRepository.countOverlappingBookings(
                        room.getId(),
                        detailReq.getCheckInTime(),
                        detailReq.getCheckOutTime(),
                        fiveMinutesAgo
                );
                if (overlapping > 0) {
                    throw new AppException(ErrorCode.ROOM_ALREADY_BOOKED);
                }
            }

            if (room == null) {
                throw new AppException(ErrorCode.ROOM_ALREADY_BOOKED);
            }

            Long hotelId = room.getFloor().getBuilding().getHotel().getId();
            BranchRoomPolicy policy = policiesByRoomType.get(room.getRoomType());
            if (policy == null) {
                policy = branchRoomPolicyRepository.findByHotelIdAndRoomType(hotelId, room.getRoomType());
                if (policy != null) {
                    policiesByRoomType.put(room.getRoomType(), policy);
                }
            }

            if (policy == null) {
                throw new AppException(ErrorCode.BRANCH_POLICY_NOT_FOUND);
            }

            // 1. TÃ­nh tiá»n phá»¥ thu (ngÆ°á»i lá»›n/tráº» em) cho 1 Ä‘Ãªm tá»« Policy
            ExtraFeeBreakdownResponse extraFeeBreakdown = roomPricingCalculator.calculateExtraFeeBreakdown(policy, detailReq.getNumAdults(), detailReq.getNumChildren());

            double extraAdultFeePerNight = extraFeeBreakdown.getAdultExtraFee();
            double extraChildFeePerNight = extraFeeBreakdown.getChildExtraFee();
            double totalExtraFeePerNight = extraFeeBreakdown.getTotalExtraFee();

            // 2. XÃ¡c Ä‘á»‹nh ngÃ y check-in, check-out vÃ  sá»‘ Ä‘Ãªm
            LocalDate checkInDate = detailReq.getCheckInTime().toLocalDate();
            LocalDate checkOutDate = detailReq.getCheckOutTime().toLocalDate();

            if (!checkOutDate.isAfter(checkInDate)) {
                throw new AppException(ErrorCode.INVALID_CHECKOUT_DATE);
            }

            Room roomCheck = roomRepository.findByIdWithLock(room.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

            if (roomCheck.getRoomStatus() == RoomStatus.MAINTENANCE
                    || roomCheck.getRoomStatus() == RoomStatus.CLEANING) {
                throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE_FOR_BOOKING);
            }

            long overlappingCount = bookingDetailRepository.countOverlappingBookings(
                    roomCheck.getId(),
                    detailReq.getCheckInTime(),
                    detailReq.getCheckOutTime(),
                    LocalDateTime.now().minusMinutes(5)
            );

            if (overlappingCount > 0) {
                throw new AppException(ErrorCode.ROOM_ALREADY_BOOKED);
            }
            long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            if (nights <= 0) nights = 1;

            // 3. VÃ²ng láº·p quÃ©t tá»«ng ngÃ y (Daily Rate) Ä‘á»ƒ cá»™ng dá»“n tiá»n phÃ²ng
            double totalRoomAmountForStay = 0.0;
            double accumulatedBasePrice = 0.0; // DÃ¹ng Ä‘á»ƒ tÃ­nh giÃ¡ gá»‘c trung bÃ¬nh má»—i Ä‘Ãªm
            List<BookingNightlyPrice> nightlyPrices = new ArrayList<>();
            LocalDate currentDate = checkInDate;

            while (currentDate.isBefore(checkOutDate)) {
                LocalDate stayDate = currentDate;
                Optional<RoomSeasonalRate> seasonalRateOpt = seasonalRatesByRoomType
                        .getOrDefault(room.getRoomType(), List.of())
                        .stream()
                        .filter(rate -> !stayDate.isBefore(rate.getStartDate())
                                && !stayDate.isAfter(rate.getEndDate()))
                        .findFirst();

                double dailyRoomPrice;
                double amenitiesPrice = room.getTotalAmenitiesPrice() != null ? room.getTotalAmenitiesPrice() : 0.0;
                if (seasonalRateOpt.isPresent()) {
                    dailyRoomPrice = seasonalRateOpt.get().getPrice() + amenitiesPrice;
                } else {
                    double basePrice = policy.getBasePrice() != null ? policy.getBasePrice() : 0.0;
                    dailyRoomPrice = basePrice + amenitiesPrice;
                }

                accumulatedBasePrice += dailyRoomPrice;
                // Tiá»n phÃ²ng ngÃ y Ä‘Ã³ + phá»¥ thu Ä‘Ãªm Ä‘Ã³
                BookingNightlyPrice nightlyPrice = new BookingNightlyPrice();
                nightlyPrice.setStayDate(currentDate);
                nightlyPrice.setRoomBasePrice(BigDecimal.valueOf(dailyRoomPrice));
                nightlyPrice.setExtraAdultFee(BigDecimal.valueOf(extraAdultFeePerNight));
                nightlyPrice.setExtraChildFee(BigDecimal.valueOf(extraChildFeePerNight));
                nightlyPrice.setExtraAdultUnitPrice(BigDecimal.valueOf(policy.getExtraAdultFee()));
                nightlyPrice.setExtraChildUnitPrice(BigDecimal.valueOf(policy.getExtraChildFee()));
                nightlyPrice.setStandardCapacity(policy.getStandardCapacity());
                nightlyPrice.setMaxExtraGuests(policy.getMaxExtraGuests());
                nightlyPrice.setNightTotal(BigDecimal.valueOf(dailyRoomPrice)
                        .add(BigDecimal.valueOf(totalExtraFeePerNight))
                        .setScale(2, java.math.RoundingMode.HALF_UP));
                nightlyPrices.add(nightlyPrice);

                currentDate = currentDate.plusDays(1);
            }

            // TÃ­nh giÃ¡ phÃ²ng gá»‘c trung bÃ¬nh 1 Ä‘Ãªm
            double baseRoomPricePerNight = accumulatedBasePrice / nights;
            double roomSubTotal = nightlyPrices.stream()
                    .map(BookingNightlyPrice::getNightTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .doubleValue();

            // 4. Xá»­ lÃ½ dá»‹ch vá»¥ Ä‘i kÃ¨m vÃ  tÃ­nh tá»•ng tiá»n dá»‹ch vá»¥
            List<BookingServiceDetail> serviceDetails = processAndAttachServices(null, detailReq.getServiceRequests());
            double serviceSubTotal = serviceDetails.stream().filter(sd -> !Boolean.TRUE.equals(sd.getIsPaid())).mapToDouble(sd -> sd.getPrice() * sd.getQuantity()).sum(); //

            // cap nhat trang thai dich vu da duoc cong tien
            markServicesAsProcessedOrPaid(serviceDetails);
            // 5. Tá»•ng cá»™ng cuá»‘i cÃ¹ng cá»§a phÃ²ng nÃ y (PhÃ²ng + Dá»‹ch vá»¥)
            double totalPrice = roomSubTotal + serviceSubTotal;

            // 6. XÃ¢y dá»±ng Entity BookingDetail Ä‘áº§y Ä‘á»§ cÃ¡c khoáº£n chi tiáº¿t
            BookingDetail bookingDetail = BookingDetail.builder().booking(booking).room(room).checkinTime(detailReq.getCheckInTime()).checkoutTime(detailReq.getCheckOutTime()).numAdults(detailReq.getNumAdults()).numChildren(detailReq.getNumChildren()).baseRoomPricePerNight(baseRoomPricePerNight).extraAdultFeePerNight(extraAdultFeePerNight).extraChildFeePerNight(extraChildFeePerNight).roomSubTotal(roomSubTotal).serviceSubTotal(serviceSubTotal).totalPrice(totalPrice).status(BookingStatusType.PENDING).build();

            nightlyPrices.forEach(bookingDetail::addNightlyPrice);
            if (!serviceDetails.isEmpty()) {
                serviceDetails.forEach(sd -> sd.setBookingDetail(bookingDetail));
                bookingDetail.setBookingServiceDetails(serviceDetails);
            }

            booking.addBookingDetail(bookingDetail);
            return bookingDetail;
        }).toList();
    }

    // HaÌ€m cÃ¢Ì£p nhÃ¢Ì£t nhÆ°Ìƒng diÌ£ch vuÌ£ Ä‘aÌƒ Ä‘Æ°Æ¡Ì£c tiÌnh tiÃªÌ€n
    private void markServicesAsProcessedOrPaid(List<BookingServiceDetail> serviceDetails) {
        if (serviceDetails == null || serviceDetails.isEmpty()) {
            return;
        }
        for (BookingServiceDetail sd : serviceDetails) {
            // Chá»‰ cáº­p nháº­t nhá»¯ng mÃ³n chÆ°a thanh toÃ¡n
            if (!Boolean.TRUE.equals(sd.getIsPaid())) {
                sd.setIsPaid(true);
            }
        }
        log("Đã cập nhật xong những dịch vụ đã được tính tiền");
    }

    // Ham nay sÆ°Ì‰ duÌ£ng khi checkout tiÌnh tiÃªÌ€n phÃ¢Ì€n diÌ£ch vuÌ£ chÆ°a Ä‘Æ°Æ¡Ì£c tiÌnh tiÃªÌ€n
    @Override
    public CheckoutSummaryResponse getCheckoutSummary(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId).get();
        Order order = booking.getOrder();

        // Lá»c ra táº¥t cáº£ cÃ¡c dá»‹ch vá»¥ CHÆ¯A thanh toÃ¡n (isPaid = false)
        List<BookingServiceDetail> unpaidServices = booking.getBookingDetails().stream().flatMap(detail -> detail.getBookingServiceDetails().stream()).filter(service -> !service.getIsPaid()).toList();

        List<UnpaidServiceItemResponse> unpaidServiceItemResponseList = unpaidServices.stream().map(v -> UnpaidServiceItemResponse.builder().bookingServiceId(v.getId()).serviceName(v.getService() != null ? v.getService().getName() : null) // Thay tÃªn hÃ m get tÃ¹y theo Entity Service cá»§a báº¡n (vÃ­ dá»¥: getName())
                .quantity(v.getQuantity()).price(v.getPrice()).subTotal(v.getPrice() != null ? v.getPrice() * v.getQuantity() : 0.0).usedAt(v.getUsedAt()).build()).toList();
        BigDecimal unpaidServiceTotal = unpaidServices.stream().map(s -> BigDecimal.valueOf(s.getPrice()).multiply(BigDecimal.valueOf(s.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);


        return CheckoutSummaryResponse.builder().orderId(order.getId()).unpaidServices(unpaidServiceItemResponseList).remainingAmountToPay(unpaidServiceTotal) // Sá»‘ tiá»n thá»±c táº¿ cáº§n tráº£ lÃºc checkout
                .build();
    }

    // ham add dich vu vao booking
    @Override
    public List<BookingServiceDetail> processAndAttachServices(BookingDetail bookingDetail, List<BookingServiceRequest> serviceRequests) {
        if (serviceRequests == null || serviceRequests.isEmpty()) {
            return List.of();
        }

        return serviceRequests.stream().map(servReq -> {
            iuh.fit.se.hotelmanagement_be.modular.service.entities.Service service = serviceRepository.findById(servReq.getServiceId()).orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_FOUND));

            BigDecimal unitPrice = servReq.getPrice() != null ? BigDecimal.valueOf(servReq.getPrice()) : BigDecimal.valueOf(service.getPrice());
            return BookingServiceDetail.builder().bookingDetail(bookingDetail).service(service).name(servReq.getName()).quantity(servReq.getQuantity()).price(unitPrice.doubleValue()).used(false).isPaid(false).build();
        }).collect(Collectors.toList());
    }

    @Override
    public BigDecimal calculateTotalRoomPrice(List<BookingDetail> details) {
        return details.stream().map(d -> BigDecimal.valueOf(d.getRoomSubTotal())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateTotalServicePrice(List<BookingDetail> bookingDetails) {
        if (bookingDetails == null) return BigDecimal.ZERO;

        return bookingDetails.stream().filter(detail -> detail.getBookingServiceDetails() != null).flatMap(detail -> detail.getBookingServiceDetails().stream()).map(serviceDetail -> BigDecimal.valueOf(serviceDetail.getPrice()).multiply(BigDecimal.valueOf(serviceDetail.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PromotionDiscountResult applyPromotion(String customerId, String promoCode, BigDecimal currentTotalAmount, BigDecimal roomTotal, BigDecimal serviceTotal, Booking booking) {

        // 1. Náº¿u khÃ´ng cÃ³ mÃ£ khuyáº¿n mÃ£i, tráº£ vá» káº¿t quáº£ báº±ng 0 cho táº¥t cáº£ cÃ¡c cá»™t
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
            // TÃ­nh toÃ¡n cho khuyáº¿n mÃ£i riÃªng cá»§a khÃ¡ch hÃ ng
            CustomerPromotion customerPromotion = customerPromoOtp.get();

            if (customerPromotion.isUsed()) {
                throw new AppException(ErrorCode.PROMOTION_ALREADY_USED);
            }
            if (customerPromotion.getBooking() != null
                    && (booking.getId() == null
                    || !customerPromotion.getBooking().getId().equals(booking.getId()))) {
                throw new AppException(ErrorCode.PROMOTION_ALREADY_USED);
            }

            Promotion promotion = customerPromotion.getPromotion();
            validatePromotionRules(promotion, currentTotalAmount, now, roomTotal, serviceTotal);

            customerPromotion.setUsed(true);
            customerPromotion.setUsedAt(now);
            customerPromotion.setBooking(booking);

            return calculateDiscountBreakdown(promotion, roomTotal, serviceTotal);
        }

        // 2. Khuyáº¿n mÃ£i chung cá»§a há»‡ thá»‘ng / chi nhÃ¡nh
        Promotion promotion = promotionRepository.findByCodeAndDeletedFalse(promoCode)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        validatePromotionRules(promotion, currentTotalAmount, now, roomTotal, serviceTotal);

        return calculateDiscountBreakdown(promotion, roomTotal, serviceTotal);
    }

    @Override
    public void validatePromotionRules(Promotion promotion, BigDecimal currentTotalAmount, LocalDateTime now, BigDecimal roomTotal, BigDecimal serviceTotal) {
        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            throw new AppException(ErrorCode.PROMOTION_INACTIVE);
        }

        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            throw new AppException(ErrorCode.PROMOTION_EXPIRED);
        }

        if (promotion.getUsageLimit() != null
                && (promotion.getUsedCount() == null ? 0 : promotion.getUsedCount()) >= promotion.getUsageLimit()) {
            throw new AppException(ErrorCode.PROMOTION_OUT_OF_STOCK);
        }

        // Kiá»ƒm tra ngÆ°á»¡ng tá»•ng Ä‘Æ¡n tá»‘i thiá»ƒu
        if (promotion.getMinBookingValue() != null && currentTotalAmount.compareTo(promotion.getMinBookingValue()) < 0) {
            throw new AppException(ErrorCode.PROMOTION_MIN_ORDER_NOT_MET);
        }

        // Kiá»ƒm tra ngÆ°á»¡ng tiá»n phÃ²ng tá»‘i thiá»ƒu (Má»šI)
        if (promotion.getMinRoomValue() != null && roomTotal.compareTo(promotion.getMinRoomValue()) < 0) {
            throw new AppException(ErrorCode.PROMOTION_MIN_ROOM_NOT_MET);
        }

        // Kiá»ƒm tra ngÆ°á»¡ng tiá»n dá»‹ch vá»¥ tá»‘i thiá»ƒu (Má»šI)
        if (promotion.getMinServiceValue() != null && serviceTotal.compareTo(promotion.getMinServiceValue()) < 0) {
            throw new AppException(ErrorCode.PROMOTION_MIN_SERVICE_NOT_MET);
        }
    }


    public void validatePromotionRulesPromotionOld(Promotion promotion, BigDecimal currentTotalAmount, LocalDateTime now, BigDecimal roomTotal, BigDecimal serviceTotal) {
        // A promotion already reserved by this booking keeps working after expiry or quota exhaustion.
        if (promotion.getMinBookingValue() != null && currentTotalAmount.compareTo(promotion.getMinBookingValue()) < 0) {
            throw new AppException(ErrorCode.PROMOTION_MIN_ORDER_NOT_MET);
        }

        // Kiá»ƒm tra ngÆ°á»¡ng tiá»n phÃ²ng tá»‘i thiá»ƒu (Má»šI)
        if (promotion.getMinRoomValue() != null && roomTotal.compareTo(promotion.getMinRoomValue()) < 0) {
            throw new AppException(ErrorCode.PROMOTION_MIN_ROOM_NOT_MET);
        }

        // Kiá»ƒm tra ngÆ°á»¡ng tiá»n dá»‹ch vá»¥ tá»‘i thiá»ƒu (Má»šI)
        if (promotion.getMinServiceValue() != null && serviceTotal.compareTo(promotion.getMinServiceValue()) < 0) {
            throw new AppException(ErrorCode.PROMOTION_MIN_SERVICE_NOT_MET);
        }
    }

    @Override
    public PromotionDiscountResult calculateDiscountBreakdown(Promotion promotion, BigDecimal roomTotal, BigDecimal serviceTotal) {
        BigDecimal roomDiscount = BigDecimal.ZERO;
        BigDecimal serviceDiscount = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        roomTotal = roomTotal != null ? roomTotal : BigDecimal.ZERO;
        serviceTotal = serviceTotal != null ? serviceTotal : BigDecimal.ZERO;
        BigDecimal absoluteTotal = roomTotal.add(serviceTotal); // tong tien (tien phong + tien dich vu)

        // 1. TÃ­nh sá»‘ tiá»n giáº£m thÃ´ dá»±a trÃªn loáº¡i mÃ£ khuyáº¿n mÃ£i
        BigDecimal rawDiscount = switch (promotion.getType()) {
            case ROOM -> calculateBaseDiscount(roomTotal, promotion);
            case SERVICE -> calculateBaseDiscount(serviceTotal, promotion);
            case TOTAL -> calculateBaseDiscount(absoluteTotal, promotion);
            default -> BigDecimal.ZERO;
        };

        // 2. Cháº·n tráº§n giáº£m giÃ¡ tá»‘i Ä‘a (maxDiscountAmount) náº¿u cÃ³ cáº¥u hÃ¬nh
        if (promotion.getMaxDiscountAmount() != null && rawDiscount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
            rawDiscount = promotion.getMaxDiscountAmount();
        }

        // 3. Äáº£m báº£o tiá»n giáº£m khÃ´ng vÆ°á»£t quÃ¡ tá»•ng tiá»n thá»±c táº¿ cá»§a hÃ³a Ä‘Æ¡n
        rawDiscount = rawDiscount.min(absoluteTotal);

        // 4. PhÃ¢n tÃ¡ch rÃµ rÃ ng vÃ o cÃ¡c biáº¿n tÆ°Æ¡ng á»©ng tÃ¹y theo loáº¡i mÃ£
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
//                discountAmount = BigDecimal.ZERO;
                break;
        }

        // Tráº£ vá» Ä‘á»‘i tÆ°á»£ng class chá»©a Ä‘áº§y Ä‘á»§ chi tiáº¿t tá»«ng khoáº£n giáº£m
        return PromotionDiscountResult.builder()
                .discountRoomAmount(roomDiscount)
                .discountServiceAmount(serviceDiscount)
                .discountAmountTotal(totalDiscount)
                .build();
    }

    @Override
    // HÃ m phá»¥ trá»£ Ä‘á»ƒ tÃ­nh tiá»n giáº£m thÃ´ theo % hoáº·c sá»‘ tiá»n máº·t cá»‘ Ä‘á»‹nh
    public BigDecimal calculateBaseDiscount(BigDecimal baseAmount, Promotion promotion) {
        if (promotion.getDiscountType() == PromotionDiscountType.PERCENTAGE) {
            return baseAmount.multiply(promotion.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        } else if (promotion.getDiscountType() == PromotionDiscountType.FIXED_AMOUNT) {
            return promotion.getDiscountValue(); // Giáº£m sá»‘ tiá»n máº·t cá»‘ Ä‘á»‹nh (VD: giáº£m tháº³ng 50k)
        }
        return BigDecimal.ZERO;
    }


    /**
     * 3. NGHIỆP VỤ THÊM DỊCH VỤ VÀO PHÒNG (Room Charge - TÃ­nh tiá»n ná»‘t lÃºc Checkout)
     */


    @Override
    public BookingResponseForHotel toBookingForHotelResponse(Booking booking) {
        return toBookingForHotelResponse(booking, BigDecimal.ZERO, null);
    }

    private BookingResponseForHotel toBookingForHotelResponse(
            Booking booking,
            BigDecimal refundedAmount,
            BookingCancellation cancellation) {
        if (booking == null) {
            return null;
        }

        List<BookingDetailResponseForHotel> detailResponses = null;

        if (booking.getBookingDetails() != null) {
            detailResponses = booking.getBookingDetails().stream().map(detail -> BookingDetailResponseForHotel.builder().bookingStatusType(detail.getStatus())
                    .cancelledAt(detail.getCancelledAt())
                    .roomNumber(detail.getRoom().getRoomNumber()).bookingDetailId(detail.getId()).roomId(detail.getRoom() != null ? detail.getRoom().getId() : null).roomName(detail.getRoom() != null ? detail.getRoom().getRoomType().toString() : null).roomTypeName(detail.getRoom() != null ? detail.getRoom().getRoomType().name() : null).checkInTime(detail.getCheckinTime()).checkOutTime(detail.getCheckoutTime()).numAdults(detail.getNumAdults()).numChildren(detail.getNumChildren())
                    // Map tháº³ng cÃ¡c khoáº£n chi tiáº¿t vÃ o Response
                    .baseRoomPricePerNight(detail.getBaseRoomPricePerNight())
                    .extraAdultFeePerNight(detail.getExtraAdultFeePerNight())
                    .extraChildFeePerNight(detail.getExtraChildFeePerNight())
                    .roomSubTotal(detail.getRoomSubTotal()) // tong tien phong
                    .serviceSubTotal(detail.getServiceSubTotal()) // tong tien dich vu
                    .totalPrice(detail.getTotalPrice()) // tong tien (dich vu + tien phong)
                    .nightlyPrices(detail.getNightlyPrices() == null ? List.of() : detail.getNightlyPrices().stream().map(price -> BookingNightlyPriceResponse.builder()
                            .stayDate(price.getStayDate())
                            .roomBasePrice(price.getRoomBasePrice())
                            .extraAdultFee(price.getExtraAdultFee())
                            .extraChildFee(price.getExtraChildFee())
                            .extraAdultUnitPrice(price.getExtraAdultUnitPrice())
                            .extraChildUnitPrice(price.getExtraChildUnitPrice())
                            .standardCapacity(price.getStandardCapacity())
                            .maxExtraGuests(price.getMaxExtraGuests())
                            .nightTotal(price.getNightTotal())
                            .build()).toList())
                    .earlyCheckInFee(detail.getEarlyCheckInFee())
                    .lateCheckOutFee(detail.getLateCheckOutFee())
                    // Thoi gian checkin - checkout thuc te
                    .actualCheckOutTime(detail.getActualCheckOutTime())
                    .actualCheckInTime(detail.getActualCheckInTime())
                    // map phan khuyen mai

                    // map danh sach dich vu
                    .bookingServiceResponsForHotels(detail.getBookingServiceDetails() != null ? detail.getBookingServiceDetails().stream().map(serviceDetail -> BookingServiceResponseForHotel.builder()
                            .bookingServiceDetailId(serviceDetail.getId())
                            .serviceId(serviceDetail.getService() != null ? serviceDetail.getService().getId() : null)
                            .name(serviceDetail.getName())
                            .quantity(serviceDetail.getQuantity())
                            .cancelled(serviceDetail.getCancelled())
                            .cancelledAt(serviceDetail.getCancelledAt())
                            .price(serviceDetail.getPrice())
                            .subTotal(serviceDetail.getPrice() == null
                                    ? 0.0
                                    : serviceDetail.getPrice() * serviceDetail.getQuantity())
                            .usedAt(serviceDetail.getUsedAt())
                            .used(serviceDetail.getUsed())
                            .usedBy(serviceDetail.getUsedBy())
                            .build()).toList() : null).build()).toList();
        }
        Order order = booking.getOrder();
        return BookingResponseForHotel.builder()
                .orderId(order.getId())
                .bookingId(booking.getId())
                .customerId(booking.getCustomer() != null ? booking.getCustomer().getId() : null)
                .customerName(booking.getCustomer() != null ? booking.getCustomer().getFullName() : null)
                .cccd(booking.getCustomer() != null ? booking.getCustomer().getCccd() : null)
                .bookingStatus(booking.getBookingStatus()).bookingChannel(booking.getBookingChannel())
                .createdAt(booking.getCreatedAt())
                .cancellationReason(cancellation == null ? null : cancellation.getReason())
                .cancelledAt(cancellation == null ? null : cancellation.getCancelledAt())
                .roomTotal(order != null ? order.getRoomTotalAmount() : null)
                // map phan khuyen mai
                .promotionId(booking.getPromotion() != null ? booking.getPromotion().getId() : null)
                .customerPromotionId(booking.getCustomerPromotion() != null ? booking.getCustomerPromotion().getId() : null)
                .serviceTotal(order != null ? order.getServiceTotalAmount() : null)
                .surchargeTotalAmount(order.getSurchargeTotalAmount()) // tong tien phu thu
                .applyAt(booking.getApplyAt() != null ? booking.getApplyAt() : null)
                .discountTotal(order != null ? order.getDiscountAmountTotal() : null) // cai nay la cai field lay tien giam gia ( dinh nghia chung)
                .paidAmount(order != null ? order.getPaidAmount() : null) // tien da thanh toaÌn
                .collectedAmount(order == null
                        ? BigDecimal.ZERO
                        : (order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount()).add(refundedAmount))
                .refundedAmount(refundedAmount)
                .remainingAmount(order != null ? order.getRemainingAmount() : null) // so tien con lai
                .finalAmount(order != null ? order.getTotalAmount() : null) // tien tong hoa don
                // lam ro phan tien giam gia cho phong / dich vu / ca phong va dich vu
                .discountRoomAmount(order != null ? order.getDiscountRoomAmount() : null)     // Giảm giá phòng
                .discountServiceAmount(order != null ? order.getDiscountServiceAmount() : null) // Giảm giá dịch vụ
                .discountAmountTotal(order != null ? order.getDiscountAmountTotal() : null)
                .estimatedDiscountAmount(booking.getApplyAmount() != null
                        ? booking.getApplyAmount()
                        : order != null
                        ? (order.getDiscountAmountTotal() != null ? order.getDiscountAmountTotal() : BigDecimal.ZERO)
                                .add(order.getDiscountRoomAmount() != null ? order.getDiscountRoomAmount() : BigDecimal.ZERO)
                                .add(order.getDiscountServiceAmount() != null ? order.getDiscountServiceAmount() : BigDecimal.ZERO)
                        : BigDecimal.ZERO)
                .bookingDetails(detailResponses).build();
    }

    @Override
    @Transactional
    public List<BookingResponseForHotel> getBookingsByHotel(Long hotelId) {
        // Gá»i repository láº¥y danh sÃ¡ch booking theo hotelId
        List<Booking> bookings = bookingRepository.findAllByHotelId(hotelId);
        Map<String, BigDecimal> refundedAmounts = paymentRepository
                .findRefundTotalsByHotelId(hotelId, CashFlowType.REFUND)
                .stream()
                .collect(Collectors.toMap(
                        PaymentRepository.OrderRefundTotalProjection::getOrderId,
                        PaymentRepository.OrderRefundTotalProjection::getRefundedAmount));
        Map<String, BookingCancellation> cancellationsByBooking = bookingCancellationRepository
                .findAllByBookingHotelId(hotelId)
                .stream()
                .collect(Collectors.toMap(
                        cancellation -> cancellation.getBooking().getId(),
                        cancellation -> cancellation));

        // Chuyá»ƒn Ä‘á»•i sang danh sÃ¡ch BookingResponse
        return bookings.stream()
                .map(booking -> toBookingForHotelResponse(
                        booking,
                        booking.getOrder() == null
                                ? BigDecimal.ZERO
                                : refundedAmounts.getOrDefault(booking.getOrder().getId(), BigDecimal.ZERO),
                        cancellationsByBooking.get(booking.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public List<RoomMatrixResponse> getRoomMatrix(Long hotelId, LocalDate startDate, LocalDate endDate) {
        List<Room> allRooms = roomRepository.findByFloor_Building_Hotel_Id(hotelId);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        List<BookingDetail> activeDetails = bookingDetailRepository.findCalendarBookingsByDateRange(
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
                        // 1. Æ¯u tiÃªn láº¥y má»‘c thá»i gian thá»±c táº¿ náº¿u cÃ³, náº¿u chÆ°a cÃ³ thÃ¬ dÃ¹ng giá» dá»± kiáº¿n
                        LocalDateTime effectiveCheckIn = detail.getCheckinTime();
                        LocalDateTime effectiveCheckOut = detail.getCheckoutTime();

                        // 2. TÃ­nh toÃ¡n danh sÃ¡ch ngÃ y bá»‹ chiáº¿m (occupiedDates) dá»±a trÃªn má»‘c thá»±c táº¿ nÃ y
                        List<LocalDate> dates = new ArrayList<>();
                        if (effectiveCheckIn != null && effectiveCheckOut != null) {
                            LocalDate current = effectiveCheckIn.toLocalDate();
                            LocalDate end = effectiveCheckOut.toLocalDate();
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
                                .checkinTime(effectiveCheckIn)   // Tráº£ vá» giá» thá»±c táº¿ lÃªn matrix
                                .checkoutTime(effectiveCheckOut) // Tráº£ vá» giá» thá»±c táº¿ lÃªn matrix
                                .bookingStatus(detail.getBooking() != null ? detail.getBooking().getBookingStatus() : null)
                                .roomStatus(detail.getStatus())
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

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account account) {
        if (account == null) {
            return List.of();
        }
        String customerId = null;
        String email = account.getEmail();
        if (account.getCustomer() != null) {
            customerId = account.getCustomer().getId();
            if (email == null) {
                email = account.getCustomer().getEmail();
            }
        } else {
            final String lookupEmail = email;
            java.util.Optional<Customer> opt = customerRepository.findFirstByAccount(account);
            if (opt.isEmpty() && lookupEmail != null) {
                opt = customerRepository.findFirstByEmail(lookupEmail);
            }
            if (opt.isPresent()) {
                customerId = opt.get().getId();
                if (email == null) {
                    email = opt.get().getEmail();
                }
            }
        }
        if (customerId == null && email == null) {
            return List.of();
        }
        List<Booking> bookings = bookingRepository.findByCustomerIdOrEmail(customerId, email);
        return bookings.stream().map(this::toBookingResponse).toList();
    }
}