package iuh.fit.se.hotelmanagement_be.modular.branch.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BranchSocketEmitter {
    private final SocketIOServer socketIOServer;

    /**
     * 4. Bắn sự kiện cập nhật giá hoặc chính sách phòng cho nhân viên và khách hàng đang xem chi nhánh
     */
    public void emitRoomPolicyUpdate(Long hotelId, Object policyData) {
        if (hotelId != null) {
            String roomName = "hotel_" + hotelId;
            socketIOServer.getRoomOperations(roomName)
                    .sendEvent("room_policy_updated", policyData);
            log.info("💰 [Branch Socket] Đã gửi thông báo thay đổi giá/chính sách 'room_policy_updated' tới room chi nhánh: {}", roomName);
        }
    }
}
