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
public class BranchSocketEmitter {
    SocketIOServer socketIOServer;
    NotificationService notificationService;

    public void emitRoomPolicyUpdate(Long hotelId, Object policyData) {
        emitRoomPolicyChanged(hotelId, "UPDATED", policyData);
    }

    public void emitRoomPolicyChanged(Long hotelId, String action, Object policyData) {
        if (hotelId == null) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelId", hotelId);
        payload.put("action", action);
        payload.put("data", policyData);
        payload.put("occurredAt", Instant.now().toString());

        try {
            boolean created = "CREATED".equalsIgnoreCase(action);
            notificationService.createAndPublish(
                    hotelId,
                    created ? NotificationType.ROOM_POLICY_CREATED : NotificationType.ROOM_POLICY_UPDATED,
                    created ? "Chính sách phòng mới" : "Chính sách phòng được cập nhật",
                    created
                            ? "Cấu hình giá và sức chứa phòng mới vừa được thêm vào chi nhánh."
                            : "Cấu hình giá hoặc sức chứa phòng tại chi nhánh vừa được cập nhật.",
                    "ROOM_POLICY",
                    null);
        } catch (RuntimeException exception) {
            log.error("Could not persist room policy notification for hotel {}", hotelId, exception);
        }

        try {
            socketIOServer.getRoomOperations("hotel_" + hotelId)
                    .sendEvent("room_policy_updated", payload);
            socketIOServer.getRoomOperations("super_admin_accounts")
                    .sendEvent("room_policy_updated", payload);
            log.info("Emitted room_policy_updated ({}) for hotel {}", action, hotelId);
        } catch (RuntimeException exception) {
            log.error("Could not emit room_policy_updated ({}) for hotel {}", action, hotelId, exception);
        }
    }
}
