package iuh.fit.se.hotelmanagement_be.modular.payment.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
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
        List<Order> orders = orderRepository.findByOrderStatus(status);
        return orders.stream().map(this::toOrderResponse).toList();
    }

    @Override
    @Transactional
    public PaymentTransactionResponse processPayment(PaymentCreateRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getOrderStatus() == OrderStatusType.CLOSED) {
            throw new AppException(ErrorCode.ORDER_ALREADY_CLOSED);
        }

        BigDecimal paymentAmount = request.getAmount();
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        CashFlowType cashFlow = request.getCashFlowType() != null ? request.getCashFlowType() : CashFlowType.RECEIPT;

        if (cashFlow == CashFlowType.RECEIPT) {
            BigDecimal remaining = order.getRemainingAmount();
            if (paymentAmount.compareTo(remaining) > 0) {
                throw new AppException(ErrorCode.PAYMENT_EXCEEDS_REMAINING);
            }
        }

        // 1. Tạo bản ghi giao dịch thanh toán
        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .amount(paymentAmount)
                .paymentType(request.getPaymentType())
                .cashFlowType(cashFlow)
                .note(request.getNote())
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
        order.operation();

        // 4. Nếu đơn hàng đã hoàn tất thanh toán, cập nhật trạng thái Booking nếu đang PENDING
        Booking booking = order.getBooking();
        if (booking != null && order.getOrderStatus() == OrderStatusType.CLOSED) {
            if (booking.getBookingStatus() == BookingStatus.PENDING) {
                booking.setBookingStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);
            }
        }

        orderRepository.save(order);
        log.info("Thanh toán thành công cho Order ID {}: Số tiền = {}, Còn lại = {}, Trạng thái = {}",
                order.getId(), paymentAmount, order.getRemainingAmount(), order.getOrderStatus());

        return toPaymentTransactionResponse(savedTx);
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
