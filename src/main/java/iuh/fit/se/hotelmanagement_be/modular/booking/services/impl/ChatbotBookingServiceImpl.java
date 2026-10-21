package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.CustomerRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingDetailCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingServiceRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.ChatbotBookingRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.ChatbotBookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingService;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.ChatbotBookingService;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotBookingServiceImpl implements ChatbotBookingService {

    private final CustomerRepository customerRepository;
    private final BookingService bookingService;
    private final OrderRepository orderRepository;
    private final iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.PromotionRepository promotionRepository;

    @Override
    @Transactional
    public ChatbotBookingResponse createBookingFromChatbot(ChatbotBookingRequest request) {
        log.info("Chatbot is creating booking for customer: {}", request.getCustomerName());

        // Resolve Promotion Code to ID if present
        String actualPromoId = null;
        if (request.getPromoCode() != null && !request.getPromoCode().isEmpty()) {
            actualPromoId = promotionRepository.findByCodeAndDeletedFalse(request.getPromoCode())
                    .map(iuh.fit.se.hotelmanagement_be.modular.promotion.entities.Promotion::getId)
                    .orElse(request.getPromoCode()); // Fallback to raw value if not found, let BookingService handle the error
        }

        // 1. Find or create Customer
        Customer customer = null;
        if (request.getCustomerEmail() != null && !request.getCustomerEmail().isEmpty()) {
            customer = customerRepository.findFirstByEmail(request.getCustomerEmail().trim()).orElse(null);
        }
        if (customer == null && request.getCustomerPhone() != null && !request.getCustomerPhone().isEmpty()) {
            customer = customerRepository.findFirstByPhone(request.getCustomerPhone().trim()).orElse(null);
        }
        
        if (customer == null) {
            customer = Customer.builder()
                    .fullName(request.getCustomerName())
                    .phone(request.getCustomerPhone())
                    .email(request.getCustomerEmail())
                    .build();
            customer = customerRepository.save(customer);
        } else {
            if (request.getCustomerEmail() != null && !request.getCustomerEmail().isBlank()
                    && (customer.getEmail() == null || customer.getEmail().isBlank())) {
                customer.setEmail(request.getCustomerEmail().trim());
                customer = customerRepository.save(customer);
            }
        }

        // 2. Build Services List
        List<BookingServiceRequest> serviceRequests = new ArrayList<>();
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            serviceRequests = request.getServiceIds().stream().map(serviceId -> {
                BookingServiceRequest sReq = new BookingServiceRequest();
                sReq.setServiceId(serviceId);
                sReq.setQuantity(request.getNumAdults() != null ? request.getNumAdults() : 1);
                sReq.setUsedAt(request.getCheckInTime());
                return sReq;
            }).collect(Collectors.toList());
        }

        // 3. Build BookingDetailCreateRequest for each room
        List<BookingDetailCreateRequest> details = new ArrayList<>();
        for (String roomId : request.getRoomIds()) {
            details.add(BookingDetailCreateRequest.builder()
                    .roomId(roomId)
                    .checkInTime(request.getCheckInTime())
                    .checkOutTime(request.getCheckOutTime())
                    .numAdults(request.getNumAdults() != null ? request.getNumAdults() : 2)
                    .numChildren(request.getNumChildren() != null ? request.getNumChildren() : 0)
                    .numInfants(0)
                    .serviceRequests(serviceRequests)
                    .build());
        }

        // 4. Create Booking using standard Java BE logic
        String effectiveEmail = customer.getEmail() != null ? customer.getEmail() : request.getCustomerEmail();
        BookingCreateRequest bookingRequest = BookingCreateRequest.builder()
                .customerId(customer.getId())
                .customerEmail(effectiveEmail)
                .hotelId(request.getHotelId())
                .bookingChannel(BookingChannel.ONLINE)
                .promotionId(actualPromoId) // Global promotion
                .bookingDetails(details)
                .build();

        BookingResponse bookingResponse = bookingService.createCustomerBooking(bookingRequest);

        // 5. Retrieve the created Order to get orderCode for PayOS
        Order order = orderRepository.findByBookingId(bookingResponse.getBookingId()).orElseThrow(() ->
                new RuntimeException("Order not generated for booking: " + bookingResponse.getBookingId())
        );
        
        // HACK: IF DEMO2K, SET TOTAL AMOUNT TO 2000
        if ("DEMO2K".equalsIgnoreCase(request.getPromoCode())) {
            order.setTotalAmount(BigDecimal.valueOf(2000));
            order.setDiscountAmountTotal(order.getRoomTotalAmount().add(order.getServiceTotalAmount()).subtract(BigDecimal.valueOf(2000)));
        }

        Long orderCode = (System.currentTimeMillis() * 1000) % 1000000000L;
        if (order.getPaymentOrderCodes() == null) {
            order.setPaymentOrderCodes(new ArrayList<>());
        }
        order.getPaymentOrderCodes().add(orderCode);
        orderRepository.save(order);

        return ChatbotBookingResponse.builder()
                .bookingId(bookingResponse.getBookingId())
                .orderId(order.getId())
                .orderCode(orderCode)
                .customerId(customer.getId())
                .totalAmount(order.getTotalAmount())
                .roomId(request.getRoomIds().get(0))
                .build();
    }
}
