package iuh.fit.se.hotelmanagement_be.modular.payment.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.impl.EmailService;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.impl.BookingSocketEmitter;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.PaymentTransaction;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.OrderRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.PaymentTransactionRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.requests.PaymentCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.payment.responses.OrderResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.responses.PaymentTransactionResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.services.OrderService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    OrderRepository orderRepository;
    PaymentTransactionRepository paymentTransactionRepository;
    BookingRepository bookingRepository;
    EmailService emailService;
    BookingSocketEmitter bookingSocketEmitter;

    @Override
    public OrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return toOrderResponse(order);
    }

    @Override
    public OrderResponse getOrderByBookingId(String bookingId) {
        Order order = orderRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return toOrderResponse(order);
    }

    @Override
    public List<OrderResponse> getOrdersByStatus(OrderStatusType status) {
        List<Order> orders = (status != null)
                ? orderRepository.findByOrderStatus(status)
                : orderRepository.findAll();
        return orders.stream().map(this::toOrderResponse).toList();
    }

    @Override
    @Transactional
    public PaymentTransactionResponse processPayment(PaymentCreateRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        BigDecimal paymentAmount = request.getAmount();
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        CashFlowType cashFlow = request.getCashFlowType() != null ? request.getCashFlowType() : CashFlowType.RECEIPT;
        Booking booking = order.getBooking();
        boolean isOnlineCustomerPayment = request.getNote() != null && request.getNote().contains("trực tuyến");

        if (isOnlineCustomerPayment && cashFlow == CashFlowType.RECEIPT) {
            order.setTotalAmount(paymentAmount);
            order.setPaidAmount(BigDecimal.ZERO);
            order.setOrderStatus(OrderStatusType.OPEN);
            order.setCloseDate(null);
        } else if (order.getOrderStatus() == OrderStatusType.CLOSED) {
            throw new AppException(ErrorCode.ORDER_ALREADY_CLOSED);
        } else if (order.getOrderStatus() == OrderStatusType.CANCELLED) {
            if (booking == null || booking.getBookingStatus() != BookingStatus.CANCELLED_PAYMENT_TIMEOUT) {
                throw new AppException(ErrorCode.ORDER_ALREADY_CANCELLED);
            }
            order.setOrderStatus(OrderStatusType.OPEN);
        }

        if (cashFlow == CashFlowType.RECEIPT && !isOnlineCustomerPayment) {
            BigDecimal remaining = order.getRemainingAmount();
            if (paymentAmount.compareTo(remaining) > 0) {
                order.setTotalAmount((order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO).add(paymentAmount));
            }
        }

        // 1. Tạo bản ghi giao dịch thanh toán
        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .amount(paymentAmount)
                .paymentType(request.getPaymentType())
                .cashFlowType(cashFlow)
                .note(request.getNote())
                .transactionDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        PaymentTransaction savedTx = paymentTransactionRepository.save(transaction);

        // 2. Cập nhật số tiền đã thanh toán trong Order
        BigDecimal currentPaid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        if (cashFlow == CashFlowType.RECEIPT) {
            order.setPaidAmount(currentPaid.add(paymentAmount));
        } else {
            order.setPaidAmount(currentPaid.subtract(paymentAmount).max(BigDecimal.ZERO));
        }

        // 3. Kiểm tra tự động đóng hóa đơn nếu đã trả hết
        order.setRemainingAmount(order.getRemainingAmount());
        order.setPaymentStatus(order.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0
                ? iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentStatus.PAID
                : iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentStatus.PARTIAL);
        order.operation();

        // 4. Cập nhật trạng thái Booking sang CONFIRMED nếu đang PENDING hoặc bị auto-cancel
        if (booking != null && cashFlow == CashFlowType.RECEIPT) {
            if (booking.getBookingStatus() == BookingStatus.PENDING
                    || booking.getBookingStatus() == BookingStatus.CANCELLED_PAYMENT_TIMEOUT) {
                booking.setBookingStatus(BookingStatus.CONFIRMED);
                if (booking.getBookingDetails() != null) {
                    booking.getBookingDetails().forEach(detail -> {
                        if (detail.getStatus() == iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType.CANCELLED) {
                            detail.setStatus(iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType.PENDING);
                            detail.setCancelledAt(null);
                        }
                    });
                }
                bookingRepository.save(booking);
            }
        }

        orderRepository.save(order);
        if (cashFlow == CashFlowType.RECEIPT) {
            bookingSocketEmitter.emitPaymentReceived(order, paymentAmount, request.getPaymentType());
            sendPaymentEmailAfterCommit(order, savedTx, paymentAmount);
        }
        log.info("Thanh toán thành công cho Order ID {}: Số tiền = {}, Còn lại = {}, Trạng thái = {}",
                order.getId(), paymentAmount, order.getRemainingAmount(), order.getOrderStatus());

        return toPaymentTransactionResponse(savedTx);
    }

    private void sendPaymentEmailAfterCommit(Order order, PaymentTransaction transaction, BigDecimal amount) {
        Booking booking = order.getBooking();
        if (booking == null || booking.getCustomer() == null) {
            log.warn("Bỏ qua email thanh toán cho order {} vì không tìm thấy khách hàng", order.getId());
            return;
        }
        String email = emailService.getCustomerEmail(booking.getCustomer());
        if (email == null) {
            log.info("Bỏ qua email thanh toán cho booking {} vì khách hàng chưa có email", booking.getId());
            return;
        }

        String customerName = booking.getCustomer().getFullName();
        String bookingId = booking.getId();
        String orderId = order.getId();
        String hotelName = booking.getHotel() != null ? booking.getHotel().getName() : "Khách sạn";
        BigDecimal paidAmount = order.getPaidAmount();
        BigDecimal remainingAmount = order.getRemainingAmount();
        String paymentType = transaction.getPaymentType() != null
                ? transaction.getPaymentType().name()
                : "N/A";
        String paymentReference = transaction.getId() != null
                ? transaction.getId().toString()
                : transaction.getNote();
        LocalDateTime paidAt = transaction.getCreatedAt();

        emailService.sendAfterCommit("payment for booking " + bookingId, () ->
                emailService.sendPaymentReceivedEmail(
                        email,
                        customerName,
                        bookingId,
                        orderId,
                        hotelName,
                        amount,
                        paidAmount,
                        remainingAmount,
                        paymentType,
                        paymentReference,
                        paidAt
                ));
    }

    @Override
    @Transactional
    public OrderResponse closeOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        order.setOrderStatus(OrderStatusType.CLOSED);
        order.setCloseDate(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        log.info("Đã đóng hóa đơn Order ID {}", order.getId());
        return toOrderResponse(savedOrder);
    }

    private OrderResponse toOrderResponse(Order order) {
        Booking booking = order.getBooking();
        String customerName = null;
        String customerPhone = null;
        String customerId = null;
        String bookingId = null;

        if (booking != null) {
            bookingId = booking.getId();
            if (booking.getCustomer() != null) {
                customerId = booking.getCustomer().getId();
                customerName = booking.getCustomer().getFullName();
                customerPhone = booking.getCustomer().getPhone();
            }
        }

        List<PaymentTransaction> transactions = paymentTransactionRepository.findByOrderId(order.getId());
        List<PaymentTransactionResponse> txResponses = transactions != null
                ? transactions.stream().map(this::toPaymentTransactionResponse).toList()
                : Collections.emptyList();

        return OrderResponse.builder()
                .id(order.getId())
                .bookingId(bookingId)
                .customerId(customerId)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .issueDate(order.getIssueDate())
                .closeDate(order.getCloseDate())
                .orderStatus(order.getOrderStatus())
                .roomTotalAmount(order.getRoomTotalAmount())
                .serviceTotalAmount(order.getServiceTotalAmount())
                .discountRoomAmount(order.getDiscountRoomAmount())
                .discountServiceAmount(order.getDiscountServiceAmount())
                .discountAmountTotal(order.getDiscountAmountTotal())
                .totalAmount(order.getTotalAmount())
                .paidAmount(order.getPaidAmount())
                .remainingAmount(order.getRemainingAmount())
                .paymentTransactions(txResponses)
                .build();
    }

    private PaymentTransactionResponse toPaymentTransactionResponse(PaymentTransaction tx) {
        return PaymentTransactionResponse.builder()
                .id(tx.getId())
                .orderId(tx.getOrder() != null ? tx.getOrder().getId() : null)
                .amount(tx.getAmount())
                .paymentType(tx.getPaymentType() != null ? tx.getPaymentType().name() : null)
                .cashFlowType(tx.getCashFlowType())
                .note(tx.getNote())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
