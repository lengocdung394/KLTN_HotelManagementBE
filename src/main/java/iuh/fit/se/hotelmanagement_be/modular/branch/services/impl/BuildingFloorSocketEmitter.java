package iuh.fit.se.hotelmanagement_be.modular.branch.services.impl;

import com.corundumstudio.socketio.SocketIOServer;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import iuh.fit.se.hotelmanagement_be.modular.notification.services.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BuildingFloorSocketEmitter {
    SocketIOServer socketIOServer;
    NotificationService notificationService;

    public void emitBuildingChanged(Long hotelId, String action, Object building) {
        boolean created = "CREATED".equalsIgnoreCase(action);
        emit(hotelId, "building_changed", "BUILDING", action, building,
                created ? NotificationType.BUILDING_CREATED : NotificationType.BUILDING_UPDATED,
                created ? "Tòa nhà mới" : "Tòa nhà được cập nhật",
                created ? "Một tòa nhà mới vừa được thêm vào chi nhánh."
                        : "Thông tin tòa nhà tại chi nhánh vừa được cập nhật.");
    }

    public void emitFloorChanged(Long hotelId, String action, Object floor) {
        boolean created = "CREATED".equalsIgnoreCase(action);
        emit(hotelId, "floor_changed", "FLOOR", action, floor,
                created ? NotificationType.FLOOR_CREATED : NotificationType.FLOOR_UPDATED,
                created ? "Tầng mới" : "Tầng được cập nhật",
                created ? "Một tầng mới vừa được thêm vào tòa nhà."
                        : "Thông tin tầng trong tòa nhà vừa được cập nhật.");
    }

    private void emit(
            Long hotelId,
            String event,
            String entityType,
            String action,
            Object data,
            NotificationType notificationType,
            String title,
            String description) {
        if (hotelId == null) {
            log.warn("Cannot emit {} without hotelId", event);
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelId", hotelId);
        payload.put("entityType", entityType);
        payload.put("action", action);
        payload.put("data", data);
        payload.put("occurredAt", Instant.now().toString());

        try {
            notificationService.createAndPublish(
                    hotelId, notificationType, title, description, entityType, null);
        } catch (RuntimeException exception) {
            log.error("Could not persist {} notification for hotel {}", entityType, hotelId, exception);
        }

        try {
            socketIOServer.getRoomOperations("hotel_" + hotelId).sendEvent(event, payload);
            socketIOServer.getRoomOperations("super_admin_accounts").sendEvent(event, payload);
            log.info("Emitted {} ({}) for hotel {}", event, action, hotelId);
        } catch (RuntimeException exception) {
            log.error("Could not emit {} ({}) for hotel {}", event, action, hotelId, exception);
        }
    }
}
