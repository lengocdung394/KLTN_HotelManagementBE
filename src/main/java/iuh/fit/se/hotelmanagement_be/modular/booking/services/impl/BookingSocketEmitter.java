package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.LateCheckOutBookingNotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public void emitNewBookingNotification(
            Long hotelId,
            Map<String, Object> bookingData
    ) {
        if (hotelId == null) {
            log.warn("[Booking Socket] Không gửi thông báo vì hotelId bị null");
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelId", hotelId);
        payload.put("action", "CREATED");
        payload.put("data", bookingData);
        payload.put("occurredAt", Instant.now().toString());

        try {
            socketIOServer.getRoomOperations("hotel_" + hotelId)
                    .sendEvent("new_booking_notification", payload);
            socketIOServer.getRoomOperations("super_admin_accounts")
                    .sendEvent("new_booking_notification", payload);

            log.info("Emitted new_booking_notification for hotel {}", hotelId);
        } catch (RuntimeException exception) {
            log.error("Could not emit new_booking_notification for hotel {}", hotelId, exception);
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
