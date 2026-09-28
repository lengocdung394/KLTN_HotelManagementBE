package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerSocketEmitter {
    private final SocketIOServer socketIOServer;

    /**
     * Gửi thông báo khi có khách hàng mới được tạo/thêm tới nhân viên tại chi nhánh
     */
    /**
     * 4. Bắn sự kiện khi có khách hàng mới được tạo (dùng chung luồng room chi nhánh)
     */
    public void emitCustomerCreated(Long hotelId, Object customerData) {
        if (hotelId != null) {
            String roomName = "hotel_" + hotelId;
            socketIOServer.getRoomOperations(roomName)
                    .sendEvent("customer_created", customerData);
            log.info("👤 [Booking Socket] Đã gửi sự kiện 'customer_created' tới room chi nhánh: {}", roomName);
        }
    }
    /**
     * Gửi thông báo khi thông tin khách hàng được cập nhật
     */
    public void emitCustomerUpdated(Long hotelId, Object customerData) {
        if (hotelId != null) {
            String roomName = "hotel_" + hotelId;
            socketIOServer.getRoomOperations(roomName)
                    .sendEvent("customer_updated", customerData);
            log.info("👤 [Customer Socket] Đã gửi sự kiện 'customer_updated' tới room chi nhánh: {}", roomName);
        }
    }
}
