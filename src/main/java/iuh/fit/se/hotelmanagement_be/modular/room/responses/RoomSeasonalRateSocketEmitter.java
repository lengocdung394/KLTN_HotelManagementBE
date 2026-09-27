package iuh.fit.se.hotelmanagement_be.modular.room.responses;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomSeasonalRateSocketEmitter {
    private final SocketIOServer socketIOServer;

    public void emitSeasonalRateCreated(Long hotelId, Object rateData) {
        if (hotelId != null) {
            String roomName = "hotel_" + hotelId; // Hoặc dùng chung room hotel_{hotelId} tùy cách phân chia phía client
            socketIOServer.getRoomOperations(roomName)
                    .sendEvent("seasonal_rate_announcement", rateData);
            log.info("📢 [Socket] Đã gửi thông báo giá mùa vụ mới tới khách hàng tại room: {}", roomName);
        }
    }
}
