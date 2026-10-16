package iuh.fit.se.hotelmanagement_be.modular.room.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomImportSocketEmitter {
    private final SocketIOServer socketIOServer;
    private static final String SUPER_ADMIN_ROOM = "super_admin_accounts";


    public void emitImportProgress(
            Long hotelId,
            String taskId,
            int percent,
            String message,
            String status,
            List<Map<String, Object>> details,
            boolean completed) {
        if (hotelId == null) {
            log.warn("Cannot emit room import progress without hotelId");
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelId", hotelId);
        payload.put("taskId", taskId);
        payload.put("percent", percent);
        payload.put("message", message);
        payload.put("status", status);
        payload.put("completed", completed);
        payload.put("data", details == null ? List.of() : details);
        payload.put("occurredAt", Instant.now().toString());

        try {
            socketIOServer.getRoomOperations("hotel_" + hotelId)
                    .sendEvent("room_import_progress", payload);
            socketIOServer.getRoomOperations(SUPER_ADMIN_ROOM)
                    .sendEvent("room_import_progress", payload);
            log.info("Emitted room_import_progress for hotel {}: {}% ({})",
                    hotelId, percent, status);
        } catch (RuntimeException exception) {
            log.error("Could not emit room_import_progress for hotel {}", hotelId, exception);
        }
    }
}
