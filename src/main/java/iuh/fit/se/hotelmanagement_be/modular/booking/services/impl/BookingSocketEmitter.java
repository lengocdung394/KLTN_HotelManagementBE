package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingSocketEmitter {
    private final SocketIOServer socketIOServer;

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
    public void emitNewBookingNotification(Long hotelId, Object bookingData) {
        if (hotelId != null) {
            String roomName = "hotel_" + hotelId;
            socketIOServer.getRoomOperations(roomName)
                    .sendEvent("new_booking_notification", bookingData);
            log.info("🔔 [Booking Socket] Đã gửi chuông thông báo 'new_booking_notification' tới room chi nhánh: {}", roomName);
        }
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
}
