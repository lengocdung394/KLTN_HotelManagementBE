package iuh.fit.se.hotelmanagement_be.modular.service.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import iuh.fit.se.hotelmanagement_be.modular.notification.services.NotificationService;
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
public class ServiceImportSocketEmitter {

    SocketIOServer socketIOServer;
    NotificationService notificationService;

    public void emitImportProgress(
            Long hotelId,
            String taskId,
            int percent,
            String message,
            String status,
            boolean completed) {
        if (hotelId == null) {
            log.warn("Cannot emit service import progress without hotelId (taskId={})", taskId);
            return;
        }

        String roomName = "hotel_" + hotelId;
        Map<String, Object> payload = Map.of(
                "hotelId", hotelId,
                "taskId", taskId,
                "percent", percent,
                "message", message,
                "status", status,
                "completed", completed
        );
        if (completed) {
            boolean succeeded = "SUCCESS".equalsIgnoreCase(status);
            notificationService.createAndPublish(
                    hotelId,
                    succeeded ? NotificationType.SERVICE_IMPORT_COMPLETED : NotificationType.SERVICE_IMPORT_FAILED,
                    succeeded ? "Nhập dịch vụ hoàn tất" : "Nhập dịch vụ thất bại",
                    message,
                    "SERVICE_IMPORT",
                    taskId);
        }

        try {
            socketIOServer.getRoomOperations(roomName).sendEvent("service_import_progress", payload);
            socketIOServer.getRoomOperations("super_admin_accounts").sendEvent("service_import_progress", payload);
            log.info("Service import progress sent to {} and super_admin_accounts: {}% ({})",
                    roomName, percent, status);
        } catch (RuntimeException exception) {
            log.error("Could not emit service import progress for hotel {}, task {}", hotelId, taskId, exception);
        }
    }
}
