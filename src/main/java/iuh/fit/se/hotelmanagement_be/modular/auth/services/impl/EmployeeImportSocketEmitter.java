package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmployeeImportSocketEmitter {
    SocketIOServer socketIOServer;

    public void emitImportProgress(
            Long hotelId,
            String taskId,
            int percent,
            String message,
            String status,
            boolean completed) {
        if (hotelId == null) {
            log.warn("Cannot emit employee import progress without hotelId (taskId={})", taskId);
            return;
        }
        Map<String, Object> payload = Map.of(
                "hotelId", hotelId,
                "taskId", taskId,
                "percent", percent,
                "message", message,
                "status", status,
                "completed", completed
        );
        String roomName = "hotel_" + hotelId;
        try {
            socketIOServer.getRoomOperations(roomName).sendEvent("employee_import_progress", payload);
            socketIOServer.getRoomOperations("super_admin_accounts").sendEvent("employee_import_progress", payload);
        } catch (RuntimeException exception) {
            log.error("Could not emit employee import progress for hotel {}, task {}", hotelId, taskId, exception);
        }
    }
}
