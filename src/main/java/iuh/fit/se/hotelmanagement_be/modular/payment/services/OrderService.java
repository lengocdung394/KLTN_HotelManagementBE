package iuh.fit.se.hotelmanagement_be.modular.payment.services;

import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
import iuh.fit.se.hotelmanagement_be.modular.payment.requests.PaymentCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.payment.responses.OrderResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.responses.PaymentTransactionResponse;

import java.util.List;

public interface OrderService {

    OrderResponse getOrderById(String orderId);

    OrderResponse getOrderByBookingId(String bookingId);

    List<OrderResponse> getOrdersByStatus(OrderStatusType status);

    PaymentTransactionResponse processPayment(PaymentCreateRequest request);

    OrderResponse closeOrder(String orderId);
}
