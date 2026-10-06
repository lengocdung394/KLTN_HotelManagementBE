package iuh.fit.se.hotelmanagement_be.modular.service.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceImportSocketEmitter {

    private final SocketIOServer socketIOServer;

    public void emitImportProgress(
            Long hotelId,
            String taskId,
            int percent,
            String message,
            String status,
            boolean completed) {
        if (hotelId == null) return;

        String roomName = "hotel_" + hotelId;
        Map<String, Object> payload = Map.of(
                "hotelId", hotelId,
                "taskId", taskId,
                "percent", percent,
                "message", message,
                "status", status,
                "completed", completed
        );
        socketIOServer.getRoomOperations(roomName).sendEvent("service_import_progress", payload);
        log.info("Service import progress sent to {}: {}% ({})", roomName, percent, status);
    }
}
