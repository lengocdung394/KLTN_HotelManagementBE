package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingCancellationStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.LateCheckOutBookingNotificationResponse;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import iuh.fit.se.hotelmanagement_be.modular.notification.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingSocketEmitter {
    private final SocketIOServer socketIOServer;
    private final NotificationService notificationService;

    /**
     * 1. Bắn sự kiện cập nhật Lịch tổng phòng (Room Matrix) cho nhân viên chi nhánh
     */
    public void emitRoomMatrixUpdate(Long hotelId) {
        if (hotelId != null) {
            String roomName = "hotel_" + hotelId;
            socketIOServer.getRoomOperations(roomName)
                    .sendEvent("room_matrix_updated", "Lịch tổng phòng vừa được làm mới do có booking mới!");
            log.info("🏢 [Booking Socket] Đã gửi 'room_matrix_updated' tới room chi nhánh: {}", roomName);
        }
    }

    /**
     * 2. Gửi thông báo chi tiết (Notification) có đơn mới cho nhân viên chi nhánh
     */
    public void emitNewBookingNotification(
            Long hotelId,
            Booking booking,
            Object bookingData
    ) {
        if (hotelId == null || booking == null) return;

        notificationService.createAndPublish(
                hotelId,
                NotificationType.BOOKING_CREATED,
                "Booking mới",
                "Booking #" + booking.getId() + " vừa được tạo.",
                "BOOKING",
                booking.getId()
        );

        String roomName = "hotel_" + hotelId;
        socketIOServer.getRoomOperations(roomName)
                .sendEvent("new_booking_notification", bookingData);
        socketIOServer.getRoomOperations("super_admin_accounts")
                .sendEvent(
                        "new_booking_notification",
                        Map.of("hotelId", hotelId, "data", bookingData)
                );

        log.info("Đã gửi thông báo booking mới tới room {} và super_admin_accounts", roomName);
    }

    // chua co function cho huy booking
    public void emitBookingCancelled(Long hotelId, String bookingId, String reason) {
        if (hotelId == null) return;
        notificationService.createAndPublish(
                hotelId,
                NotificationType.BOOKING_CANCELLED,
                "Booking đã bị hủy",
                "Booking #" + bookingId + " đã bị hủy" + (reason == null || reason.isBlank() ? "." : ": " + reason),
                "BOOKING",
                bookingId);
    }

    // đã giải quyết xong luồng hoàn tiền do hủy booking
    public void emitRefundCompleted(Long hotelId, String bookingId, BigDecimal refundAmount) {
        if (hotelId == null) return;
        String formattedAmount = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
                .format(refundAmount);
        notificationService.createAndPublish(
                hotelId,
                NotificationType.BOOKING_REFUND_COMPLETED,
                "Đã hoàn tiền",
                "Đã xác nhận hoàn " + formattedAmount + "đ cho booking #" + bookingId + ".",
                "BOOKING_CANCELLATION",
                bookingId);
    }

    // thong bao huy booking
    public void emitRefundRequested(
            Long hotelId,
            String bookingId,
            BigDecimal refundAmount,
            BookingCancellationStatus status) {
        if (hotelId == null) return;
        String formattedAmount = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
                .format(refundAmount);
        notificationService.createAndPublish(
                hotelId,
                NotificationType.BOOKING_REFUND_REQUESTED,
                "Yêu cầu hoàn tiền mới",
                "Booking #" + bookingId + " có yêu cầu hoàn " + formattedAmount
                        + "đ (trạng thái: " + status + "). Dự kiến xử lý trong vòng 1–3 ngày.",
                "BOOKING_CANCELLATION",
                bookingId);
    }


    /**
     * 3. Gửi thông báo về cho chính Khách hàng (trạng thái đơn, thanh toán...)
     */
    public void emitCustomerBookingStatus(String userId, Object bookingData) {
        if (userId != null) {
            String roomName = "user_" + userId;
            socketIOServer.getRoomOperations(roomName)
                    .sendEvent("customer_booking_updated", bookingData);
            log.info("👤 [Booking Socket] Đã gửi trạng thái tới user room: {}", roomName);
        }
    }
    //

    public void emitLateCheckOutAlert(Long hotelId, LateCheckOutBookingNotificationResponse bookingData) {
        if (hotelId != null) {
            String roomName = "hotel_" + hotelId;
            socketIOServer.getRoomOperations(roomName)
                    .sendEvent("late_check_out_calendar", bookingData);

            log.info("[Checkout late!!!!!]");
        }
    }

    // Trong BookingSocketEmitter.java
    public void emitRoomTemporarilyLocked(Long hotelId, String roomId, String userId) {
        socketIOServer.getRoomOperations("hotel_" + hotelId)
                .sendEvent("room_locked_event", Map.of(
                        "roomId", roomId,
                        "status", "LOCKED",
                        "message", "Phòng này đang có người thao tác chọn"


                ));
    }

    void emitRoomTemporarilyLocked(
            Long hotelId,
            String roomId,
            LocalDate checkIn,
            LocalDate checkOut,
            String lockToken,
            Instant expiresAt
    ) {

    }

    void emitRoomTemporarilyUnlocked(
            Long hotelId,
            String roomId,
            LocalDate checkIn,
            LocalDate checkOut,
            String lockToken
    ) {

    }
}
