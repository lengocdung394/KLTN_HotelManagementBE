package iuh.fit.se.hotelmanagement_be.modular.room.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.responseForExcelRoom.ImportDetailItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomImportSocketEmitter {
    private final SocketIOServer socketIOServer;
    // ── Bổ sung hàm bắn sự kiện tiến trình Import Excel bằng Socket.IO ──
    public void emitImportProgress(Long hotelId, String taskId, int percent, String message, List<ImportDetailItem> details, boolean isCompleted) {
        if (hotelId != null) {
            String roomName = "hotel_" + hotelId;

            // Đóng gói dữ liệu tiến trình
            Map<String, Object> payload = Map.of(
                    "taskId", taskId,
                    "percent", percent,
                    "message", message,
                    "completed", isCompleted,
                    "data", (details != null ? details : List.of())
            );

            // Gửi sự kiện có tên là "room_import_progress" tới client đang join vào room khách sạn
            socketIOServer.getRoomOperations(roomName)
                    .sendEvent("room_import_progress", payload);

            log.info("📊 [Room Socket] Đã gửi tiến trình import tới room hotel_{}: {}% - {}", hotelId, percent, message);
        }
    }
}
