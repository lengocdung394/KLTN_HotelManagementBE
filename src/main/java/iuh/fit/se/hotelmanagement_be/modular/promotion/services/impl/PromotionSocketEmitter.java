package iuh.fit.se.hotelmanagement_be.modular.promotion.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionSocketEmitter {

    private final SocketIOServer socketIOServer;

    /**
     * 1. Bắn sự kiện cập nhật Lịch tổng phòng (Room Matrix) cho nhân viên chi nhánh
     */
    public void emitPromotionUpdate(Long hotelId) {
        if (hotelId != null) {
            String roomName = "hotel_" + hotelId;
            socketIOServer.getRoomOperations(roomName)
                    .sendEvent("update_promotion_notification", "Vừa có khuyến mãi mới");
            log.info("🏢 [Promotion Socket] Đã gửi 'room_matrix_updated' tới promotion chi nhánh: {}", roomName);
        }
    }

    /**
     * 2. Gửi thông báo chi tiết (Notification) có đơn mới cho nhân viên chi nhánh
     */
    public void emitPromotionCreate(Long hotelId, Object promotion) {
        if (hotelId != null) {
            String roomName = "hotel_" + hotelId;
            socketIOServer.getRoomOperations(roomName)
                    .sendEvent("new_promotion_notification", promotion);
            log.info("🔔 [Promotion Socket] Đã gửi chuông thông báo 'new_promotion_notification' tới promotion chi nhánh: {}", roomName);
        }
    }

    // phan tich luong khuyen mai cho khach hàng nha


}
