package iuh.fit.se.hotelmanagement_be.modular.room.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomSocketEmitter {
    private final SocketIOServer socketIOServer;

    // su kien them thanh cong phong


    public void emitRoomRoomCreate(Long hotelId, Object roomData)
    {

        if (hotelId != null) {
            String roomName = "hotel_" + hotelId;
            socketIOServer.getRoomOperations(roomName)
                    .sendEvent("room_create", "DS phong duoc lam moi do co phong moi!");
            log.info("🏢 [Room Socket] Đã gửi 'room_create' tới room chi nhánh: {}",roomData);
        }
    }

    public void emitRoomRoomUpdate(Long hotelId, Object roomData)
    {

        if (hotelId != null) {
            String roomName = "hotel_" + hotelId;
            socketIOServer.getRoomOperations(roomName)
                    .sendEvent("room_update", "DS phong duoc lam moi do co phong duoc update!");
            log.info("🏢 [Rooom Socket] Đã gửi 'room_update' tới room chi nhánh: {}",roomData);
        }
    }
    // su kien cap nhat thanh cong phong
}
