package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingServiceDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingDetailRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingServiceDetailRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.OrderRepository;
import iuh.fit.se.hotelmanagement_be.modular.service.repositories.ServiceRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public class BookingServiceManagementService {
    BookingServiceDetailRepository bookingServiceDetailRepository;
    BookingDetailRepository bookingDetailRepository;
    ServiceRepository serviceRepository; // Repository của danh mục Service
    OrderRepository orderRepository;

    /**
     * 1. Thêm dịch vụ vào phòng (Áp dụng cho cả trước hoặc trong khi đang check-in)
     */
    @Transactional
    public void addServiceToRoom(Long bookingDetailId, Long serviceId, int quantity) {
        BookingDetail bookingDetail = bookingDetailRepository.findById(bookingDetailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin phòng đặt"));

        // Lấy giá dịch vụ từ danh mục dịch vụ gốc
        iuh.fit.se.hotelmanagement_be.modular.service.entities.Service serviceCatalog = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ trong danh mục"));

        // Tạo chi tiết dịch vụ mới
        BookingServiceDetail serviceDetail = BookingServiceDetail.builder()
                .bookingDetail(bookingDetail)
                .service(serviceCatalog)
                .quantity(quantity)
                .price(serviceCatalog.getPrice()) // Lấy giá hiện tại của dịch vụ gán vào
                .isPaid(false)
                .build();

        bookingServiceDetailRepository.save(serviceDetail);

        // Tự động tính lại tổng tiền dịch vụ cho Order chung của toàn bộ booking
        recalculateAndUpdateOrderServiceTotal(bookingDetail);
    }

    /**
     * 2. Cập nhật số lượng dịch vụ (tăng/giảm)
     */
    @Transactional
    public void updateServiceQuantity(Long bookingServiceId, int newQuantity) {
        BookingServiceDetail serviceDetail = bookingServiceDetailRepository.findById(bookingServiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ phát sinh"));

        if (newQuantity <= 0) {
            deleteService(bookingServiceId);
            return;
        }

        serviceDetail.setQuantity(newQuantity);
        bookingServiceDetailRepository.save(serviceDetail);

        recalculateAndUpdateOrderServiceTotal(serviceDetail.getBookingDetail());
    }

    /**
     * 3. Xóa / Bỏ chọn dịch vụ khỏi phòng
     */
    @Transactional
    public void deleteService(Long bookingServiceId) {
        BookingServiceDetail serviceDetail = bookingServiceDetailRepository.findById(bookingServiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ phát sinh"));

        BookingDetail bookingDetail = serviceDetail.getBookingDetail();
        bookingServiceDetailRepository.delete(serviceDetail);

        recalculateAndUpdateOrderServiceTotal(bookingDetail);
    }

    /**
     * Hàm dùng chung: Quét toàn bộ dịch vụ của tất cả các phòng trong booking để tính tổng serviceTotalAmount và cập nhật vào Order
     */
    public void recalculateAndUpdateOrderServiceTotal(BookingDetail bookingDetail) {
        if (bookingDetail == null || bookingDetail.getBooking() == null) {
            return;
        }

        // Lấy Order thông qua Booking
        var booking = bookingDetail.getBooking();
        Order order = booking.getOrder();
        if (order == null) return;

        BigDecimal totalServiceAmount = BigDecimal.ZERO;

        // Duyệt qua tất cả các phòng (BookingDetail) trong Booking
        if (booking.getBookingDetails() != null) {
            for (BookingDetail detail : booking.getBookingDetails()) {
                // Duyệt qua tất cả các dịch vụ của từng phòng
                if (detail.getBookingServiceDetails() != null) {
                    for (BookingServiceDetail item : detail.getBookingServiceDetails()) {
                        if (item.getPrice() != null && item.getQuantity() > 0) {
                            BigDecimal subTotal = BigDecimal.valueOf(item.getPrice()).multiply(BigDecimal.valueOf(item.getQuantity()));
                            totalServiceAmount = totalServiceAmount.add(subTotal);
                        }
                    }
                }
            }
        }

        // Cập nhật lại vào Order
        order.setServiceTotalAmount(totalServiceAmount);
        orderRepository.save(order);
    }
}
